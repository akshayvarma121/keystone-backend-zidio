package com.zidio.keystone.service;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.request.WorkOrderRequest;
import com.zidio.keystone.dto.response.*;
import com.zidio.keystone.mapper.WorkOrderMapper;
import com.zidio.keystone.repository.*;
import com.zidio.keystone.security.KeystoneUserDetails;
import com.zidio.keystone.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final PartRepository partRepository;
    private final PartUsageRepository partUsageRepository;
    private final TimeLogRepository timeLogRepository;

    @Transactional
    public WorkOrderResponse createWorkOrder(WorkOrderRequest request) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        
        if (user.getRole() == Role.CUSTOMER && !request.getCustomerId().equals(user.getCustomerId())) {
            throw new AccessDeniedException("Cannot create work order for another customer");
        }
        if (user.getRole() == Role.TECHNICIAN) {
            throw new AccessDeniedException("Technicians cannot create work orders");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + request.getSiteId()));

        User assignedTo = null;
        if (request.getAssignedTo() != null) {
            assignedTo = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new EntityNotFoundException("Assignee not found"));
        }

        Skill requiredSkill = null;
        if (request.getRequiredSkillId() != null) {
            requiredSkill = skillRepository.findById(request.getRequiredSkillId())
                    .orElseThrow(() -> new EntityNotFoundException("Skill not found"));
        }

        WorkOrder wo = new WorkOrder();
        wo.setTitle(request.getTitle());
        wo.setDescription(request.getDescription());
        wo.setPriority(request.getPriority());
        wo.setStatus(assignedTo != null ? WorkOrderStatus.ASSIGNED : WorkOrderStatus.NEW);
        wo.setCustomer(customer);
        wo.setSite(site);
        wo.setAssignedTo(assignedTo);
        wo.setRequiredSkill(requiredSkill);
        
        wo.setSlaDueAt(computeSla(wo.getPriority()));
        
        Long seq = workOrderRepository.getNextSequence();
        String code = String.format("WO-%d-%06d", Year.now().getValue(), seq);
        wo.setCode(code);

        wo = workOrderRepository.save(wo);

        User actingUser = userRepository.getReferenceById(user.getId());
        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setWorkOrder(wo);
        history.setToStatus(wo.getStatus());
        history.setChangedBy(actingUser);
        history.setNote("Created Work Order");
        historyRepository.save(history);

        return WorkOrderMapper.toResponse(wo);
    }

    @Transactional(readOnly = true)
    public WorkOrderDetailsResponse getWorkOrder(UUID id) {
        WorkOrder wo = getAndCheckAccess(id);
        List<WorkOrderStatusHistory> history = historyRepository.findByWorkOrderIdOrderByChangedAtDesc(id);
        List<PartUsage> partsUsed = partUsageRepository.findByWorkOrderIdOrderByLoggedAtDesc(id);
        List<TimeLog> timeLogs = timeLogRepository.findByWorkOrderIdOrderByLoggedAtDesc(id);

        java.math.BigDecimal partsCost = java.math.BigDecimal.ZERO;
        List<PartUsageResponse> partsResp = new java.util.ArrayList<>();
        for (PartUsage pu : partsUsed) {
            java.math.BigDecimal total = pu.getPart().getUnitCost().multiply(java.math.BigDecimal.valueOf(pu.getQtyUsed()));
            partsCost = partsCost.add(total);
            partsResp.add(PartUsageResponse.builder()
                    .id(pu.getId())
                    .partId(pu.getPart().getId())
                    .partName(pu.getPart().getName())
                    .qtyUsed(pu.getQtyUsed())
                    .unitCost(pu.getPart().getUnitCost())
                    .totalCost(total)
                    .loggedById(pu.getLoggedBy().getId())
                    .loggedByName(pu.getLoggedBy().getName())
                    .loggedAt(pu.getLoggedAt())
                    .build());
        }

        Integer totalMinutes = 0;
        java.math.BigDecimal laborCost = java.math.BigDecimal.ZERO;
        List<TimeLogResponse> timeResp = new java.util.ArrayList<>();
        for (TimeLog tl : timeLogs) {
            totalMinutes += tl.getMinutes();
            java.math.BigDecimal rate = tl.getTechnician().getHourlyRate();
            java.math.BigDecimal cost = rate.multiply(java.math.BigDecimal.valueOf(tl.getMinutes())).divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
            laborCost = laborCost.add(cost);
            
            timeResp.add(TimeLogResponse.builder()
                    .id(tl.getId())
                    .technicianId(tl.getTechnician().getId())
                    .technicianName(tl.getTechnician().getName())
                    .minutes(tl.getMinutes())
                    .note(tl.getNote())
                    .loggedAt(tl.getLoggedAt())
                    .build());
        }

        return WorkOrderDetailsResponse.builder()
                .workOrder(WorkOrderMapper.toResponse(wo))
                .history(history.stream().map(WorkOrderMapper::toHistoryResponse).collect(Collectors.toList()))
                .partsUsed(partsResp)
                .timeLogs(timeResp)
                .partsCost(partsCost)
                .totalMinutes(totalMinutes)
                .laborCost(laborCost)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> searchWorkOrders(
            String title, WorkOrderStatus status, Priority priority,
            UUID assignedTo, UUID customerId, UUID siteId, Pageable pageable) {
        
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        UUID scopeCustomerId = customerId;
        UUID scopeAssignedTo = assignedTo;

        if (user.getRole() == Role.CUSTOMER) {
            scopeCustomerId = user.getCustomerId();
        } else if (user.getRole() == Role.TECHNICIAN) {
            scopeAssignedTo = user.getId();
        }

        Page<WorkOrder> page = workOrderRepository.searchWorkOrders(
                title, status, priority, scopeAssignedTo, scopeCustomerId, siteId, pageable);
        
        return PageResponse.of(page.map(WorkOrderMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public Map<WorkOrderStatus, List<WorkOrderResponse>> getBoard() {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        UUID scopeCustomerId = user.getRole() == Role.CUSTOMER ? user.getCustomerId() : null;
        UUID scopeAssignedTo = user.getRole() == Role.TECHNICIAN ? user.getId() : null;

        List<WorkOrder> boardItems = workOrderRepository.getBoard(scopeAssignedTo, scopeCustomerId);
        
        return boardItems.stream()
                .map(WorkOrderMapper::toResponse)
                .collect(Collectors.groupingBy(WorkOrderResponse::getStatus));
    }

    @Transactional
    public WorkOrderResponse updateWorkOrder(UUID id, WorkOrderRequest request) {
        WorkOrder wo = getAndCheckAccess(id);
        
        if (wo.getStatus() == WorkOrderStatus.CLOSED || wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot edit a CLOSED or CANCELLED work order");
        }

        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        if (user.getRole() == Role.CUSTOMER || user.getRole() == Role.TECHNICIAN) {
            throw new AccessDeniedException("Only managers or dispatchers can fully edit work orders");
        }

        wo.setTitle(request.getTitle());
        wo.setDescription(request.getDescription());
        
        if (wo.getPriority() != request.getPriority()) {
            wo.setPriority(request.getPriority());
            wo.setSlaDueAt(computeSla(wo.getPriority()));
        }

        if (!wo.getCustomer().getId().equals(request.getCustomerId())) {
            wo.setCustomer(customerRepository.getReferenceById(request.getCustomerId()));
        }
        if (!wo.getSite().getId().equals(request.getSiteId())) {
            wo.setSite(siteRepository.getReferenceById(request.getSiteId()));
        }
        
        if (request.getRequiredSkillId() != null && (wo.getRequiredSkill() == null || !wo.getRequiredSkill().getId().equals(request.getRequiredSkillId()))) {
            wo.setRequiredSkill(skillRepository.getReferenceById(request.getRequiredSkillId()));
        } else if (request.getRequiredSkillId() == null) {
            wo.setRequiredSkill(null);
        }

        if (request.getAssignedTo() != null && (wo.getAssignedTo() == null || !wo.getAssignedTo().getId().equals(request.getAssignedTo()))) {
            User newAssignee = userRepository.getReferenceById(request.getAssignedTo());
            wo.setAssignedTo(newAssignee);
            if (wo.getStatus() == WorkOrderStatus.NEW) {
                wo.setStatus(WorkOrderStatus.ASSIGNED);
            }
        }

        wo = workOrderRepository.save(wo);
        return WorkOrderMapper.toResponse(wo);
    }

    @Transactional(readOnly = true)
    public List<AssignmentCandidateResponse> getAssignmentCandidates(UUID workOrderId) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        if (user.getRole() != Role.DISPATCHER && user.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Only dispatchers or managers can view candidates");
        }

        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new EntityNotFoundException("WorkOrder not found"));

        UUID skillId = wo.getRequiredSkill() != null ? wo.getRequiredSkill().getId() : null;
        List<AssignmentCandidateProjection> projections = userRepository.getAssignmentCandidates(skillId);

        return projections.stream().map(p -> AssignmentCandidateResponse.builder()
                .technicianId(p.getTechnicianId())
                .technicianName(p.getTechnicianName())
                .hasRequiredSkill(p.getHasRequiredSkill())
                .openJobsCount(p.getOpenJobsCount())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public PartUsageResponse logPartUsage(UUID workOrderId, com.zidio.keystone.dto.request.PartUsageRequest request) {
        WorkOrder wo = getAndCheckAccess(workOrderId);
        KeystoneUserDetails userDetails = SecurityUtils.getCurrentUser();
        
        Part part = partRepository.findById(request.getPartId())
                .orElseThrow(() -> new EntityNotFoundException("Part not found"));

        if (part.getStockQty() < request.getQtyUsed()) {
            throw new com.zidio.keystone.exception.InsufficientStockException("Not enough stock for part " + part.getName());
        }

        part.setStockQty(part.getStockQty() - request.getQtyUsed());
        part = partRepository.save(part);

        User actingUser = userRepository.getReferenceById(userDetails.getId());

        PartUsage pu = new PartUsage();
        pu.setWorkOrder(wo);
        pu.setPart(part);
        pu.setQtyUsed(request.getQtyUsed());
        pu.setLoggedBy(actingUser);
        pu = partUsageRepository.save(pu);

        java.math.BigDecimal totalCost = part.getUnitCost().multiply(java.math.BigDecimal.valueOf(pu.getQtyUsed()));

        return PartUsageResponse.builder()
                .id(pu.getId())
                .partId(part.getId())
                .partName(part.getName())
                .qtyUsed(pu.getQtyUsed())
                .unitCost(part.getUnitCost())
                .totalCost(totalCost)
                .loggedById(actingUser.getId())
                .loggedByName(actingUser.getName())
                .loggedAt(pu.getLoggedAt())
                .build();
    }

    @Transactional
    public TimeLogResponse logTime(UUID workOrderId, com.zidio.keystone.dto.request.TimeLogRequest request) {
        WorkOrder wo = getAndCheckAccess(workOrderId);
        KeystoneUserDetails userDetails = SecurityUtils.getCurrentUser();
        
        if (userDetails.getRole() != Role.TECHNICIAN) {
            throw new AccessDeniedException("Only technicians can log time");
        }

        User technician = userRepository.findById(userDetails.getId()).orElseThrow();

        TimeLog tl = new TimeLog();
        tl.setWorkOrder(wo);
        tl.setTechnician(technician);
        tl.setMinutes(request.getMinutes());
        tl.setNote(request.getNote());
        tl = timeLogRepository.save(tl);

        return TimeLogResponse.builder()
                .id(tl.getId())
                .technicianId(technician.getId())
                .technicianName(technician.getName())
                .minutes(tl.getMinutes())
                .note(tl.getNote())
                .loggedAt(tl.getLoggedAt())
                .build();
    }

    private WorkOrder getAndCheckAccess(UUID id) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WorkOrder not found: " + id));

        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        if (user.getRole() == Role.CUSTOMER && !wo.getCustomer().getId().equals(user.getCustomerId())) {
            throw new AccessDeniedException("Access denied to other customer's work order");
        }
        if (user.getRole() == Role.TECHNICIAN && (wo.getAssignedTo() == null || !wo.getAssignedTo().getId().equals(user.getId()))) {
            throw new AccessDeniedException("Access denied. Work order not assigned to you");
        }
        return wo;
    }

    private OffsetDateTime computeSla(Priority priority) {
        OffsetDateTime now = OffsetDateTime.now();
        switch (priority) {
            case CRITICAL: return now.plusHours(4);
            case HIGH: return now.plusHours(8);
            case MEDIUM: return now.plusHours(24);
            case LOW: return now.plusHours(72);
            default: return now.plusHours(24);
        }
    }
}
