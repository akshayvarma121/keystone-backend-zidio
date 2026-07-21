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
    private final com.zidio.keystone.repository.AttachmentRepository attachmentRepository;
    private final com.zidio.keystone.service.storage.ObjectStorageService objectStorageService;
    private final com.zidio.keystone.repository.CommentRepository commentRepository;
    private final com.zidio.keystone.repository.NotificationRepository notificationRepository;
    private final com.zidio.keystone.repository.WorkOrderRatingRepository ratingRepository;

    @Transactional
    public WorkOrderResponse createWorkOrder(WorkOrderRequest request) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        
        if (user.getRole() == Role.CUSTOMER && !request.getCustomerId().equals(user.getCustomerId())) {
            throw new AccessDeniedException("Cannot create work order for another customer");
        }
        if (user.getRole() == Role.TECHNICIAN) {
            throw new AccessDeniedException("Technicians cannot create work orders");
        }

        User creator = userRepository.getReferenceById(user.getId());
        return internalCreateWorkOrder(request, creator);
    }
    
    @Transactional
    public WorkOrderResponse createScheduledWorkOrder(WorkOrderRequest request, User creator) {
        return internalCreateWorkOrder(request, creator);
    }

    private WorkOrderResponse internalCreateWorkOrder(WorkOrderRequest request, User creator) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + request.getSiteId()));

        User assignedTo = null;
        if (request.getAssignedTo() != null) {
            assignedTo = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new EntityNotFoundException("Assignee not found"));
        }
        
        if (creator.getRole() == Role.CUSTOMER) {
            if (!site.getCustomer().getId().equals(request.getCustomerId())) {
                throw new AccessDeniedException("Cannot create work order for a site that belongs to another customer");
            }
            assignedTo = null; // Customers cannot assign work orders
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
        wo.setCreatedBy(creator);
        
        wo.setSlaDueAt(computeSla(wo.getPriority()));
        
        Long seq = workOrderRepository.getNextSequence();
        String code = String.format("WO-%d-%06d", Year.now().getValue(), seq);
        wo.setCode(code);

        wo = workOrderRepository.save(wo);

        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setWorkOrder(wo);
        history.setToStatus(wo.getStatus());
        history.setChangedBy(creator);
        history.setNote("Created Work Order");
        historyRepository.save(history);

        return WorkOrderMapper.toResponse(wo, creator.getRole());
    }

    @Transactional(readOnly = true)
    public WorkOrderDetailsResponse getWorkOrder(UUID id) {
        WorkOrder wo = getAndCheckAccess(id);
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
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
                .workOrder(WorkOrderMapper.toResponse(wo, user.getRole()))
                .history(history.stream().map(h -> WorkOrderMapper.toHistoryResponse(h, user.getRole())).collect(Collectors.toList()))
                .partsUsed(partsResp)
                .timeLogs(timeResp)
                .partsCost(partsCost)
                .totalMinutes(totalMinutes)
                .laborCost(laborCost)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> searchWorkOrders(String q, Pageable pageable) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        String customerIdStr = user.getCustomerId() != null ? user.getCustomerId().toString() : null;
        String userIdStr = user.getId().toString();
        
        Page<String> page = workOrderRepository.fullTextSearchIds(
                q, 
                user.getRole().name(), 
                customerIdStr, 
                userIdStr, 
                pageable);
                
        List<UUID> ids = page.getContent().stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
                
        List<WorkOrder> hydrated = ids.isEmpty() ? List.of() : workOrderRepository.findByIdInWithRelations(ids);
        
        // Re-sort hydrated based on the original native query order (ids list)
        Map<UUID, WorkOrder> woMap = hydrated.stream().collect(Collectors.toMap(WorkOrder::getId, w -> w));
        List<WorkOrderResponse> content = ids.stream()
                .map(woMap::get)
                .filter(java.util.Objects::nonNull)
                .map(wo -> WorkOrderMapper.toResponse(wo, user.getRole()))
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> getWorkOrders(
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
        return PageResponse.of(page.map(wo -> WorkOrderMapper.toResponse(wo, user.getRole())));
    }

    @Transactional
    public void rateWorkOrder(UUID id, com.zidio.keystone.dto.request.WorkOrderRatingRequest request) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        if (user.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers can rate work orders");
        }

        WorkOrder wo = getAndCheckAccess(id);
        
        if (wo.getStatus() != WorkOrderStatus.CLOSED) {
            throw new IllegalStateException("Can only rate CLOSED work orders");
        }

        if (ratingRepository.existsByWorkOrderId(id)) {
            throw new IllegalStateException("Work order has already been rated");
        }

        User creator = userRepository.getReferenceById(user.getId());
        
        WorkOrderRating rating = WorkOrderRating.builder()
                .workOrder(wo)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdBy(creator)
                .build();
                
        ratingRepository.save(rating);
        
        // Update the denormalized field on WorkOrder
        wo.setSatisfactionRating(request.getRating());
        workOrderRepository.save(wo);
    }

    @Transactional(readOnly = true)
    public Map<WorkOrderStatus, List<WorkOrderResponse>> getBoard() {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        UUID scopeCustomerId = user.getRole() == Role.CUSTOMER ? user.getCustomerId() : null;
        UUID scopeAssignedTo = user.getRole() == Role.TECHNICIAN ? user.getId() : null;

        List<WorkOrder> boardItems = workOrderRepository.getBoard(scopeAssignedTo, scopeCustomerId);
        
        return boardItems.stream()
                .map(wo -> WorkOrderMapper.toResponse(wo, user.getRole()))
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
        return WorkOrderMapper.toResponse(wo, user.getRole());
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

    @Transactional
    public com.zidio.keystone.dto.response.AttachmentResponse uploadAttachment(UUID workOrderId, org.springframework.web.multipart.MultipartFile file) {
        WorkOrder wo = getAndCheckAccess(workOrderId);
        KeystoneUserDetails userDetails = SecurityUtils.getCurrentUser();
        
        if (userDetails.getRole() == Role.TECHNICIAN && (wo.getAssignedTo() == null || !wo.getAssignedTo().getId().equals(userDetails.getId()))) {
            throw new AccessDeniedException("Only the assigned technician can upload attachments");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new com.zidio.keystone.exception.InvalidFileException("File size exceeds 10MB limit");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new com.zidio.keystone.exception.InvalidFileException("Only image files are allowed");
        }

        UUID attachmentId = UUID.randomUUID();
        String storageKey = "attachments/" + wo.getId() + "/" + attachmentId + "_" + file.getOriginalFilename();

        try {
            objectStorageService.upload(storageKey, file.getInputStream(), contentType, file.getSize());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read file input stream", e);
        }

        User actingUser = userRepository.getReferenceById(userDetails.getId());

        com.zidio.keystone.domain.Attachment attachment = new com.zidio.keystone.domain.Attachment();
        attachment.setId(attachmentId);
        attachment.setWorkOrder(wo);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(contentType);
        attachment.setSizeBytes(file.getSize());
        attachment.setStorageKey(storageKey);
        attachment.setUploadedBy(actingUser);
        
        attachment = attachmentRepository.save(attachment);

        return com.zidio.keystone.dto.response.AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .sizeBytes(attachment.getSizeBytes())
                .uploadedById(actingUser.getId())
                .uploadedByName(actingUser.getName())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<com.zidio.keystone.dto.response.AttachmentResponse> getAttachments(UUID workOrderId) {
        getAndCheckAccess(workOrderId); // verify access
        return attachmentRepository.findByWorkOrderIdOrderByUploadedAtDesc(workOrderId).stream()
                .map(a -> com.zidio.keystone.dto.response.AttachmentResponse.builder()
                        .id(a.getId())
                        .fileName(a.getFileName())
                        .contentType(a.getContentType())
                        .sizeBytes(a.getSizeBytes())
                        .uploadedById(a.getUploadedBy().getId())
                        .uploadedByName(a.getUploadedBy().getName())
                        .uploadedAt(a.getUploadedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.core.io.InputStreamResource downloadAttachment(UUID workOrderId, UUID attachmentId) {
        getAndCheckAccess(workOrderId); // verify access to the work order
        com.zidio.keystone.domain.Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));
                
        if (!attachment.getWorkOrder().getId().equals(workOrderId)) {
            throw new AccessDeniedException("Attachment does not belong to this work order");
        }
        
        return new org.springframework.core.io.InputStreamResource(objectStorageService.download(attachment.getStorageKey()));
    }
    
    @Transactional(readOnly = true)
    public com.zidio.keystone.domain.Attachment getAttachmentMetadata(UUID workOrderId, UUID attachmentId) {
        getAndCheckAccess(workOrderId);
        com.zidio.keystone.domain.Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));
                
        if (!attachment.getWorkOrder().getId().equals(workOrderId)) {
            throw new AccessDeniedException("Attachment does not belong to this work order");
        }
        return attachment;
    }

    @Transactional
    public com.zidio.keystone.dto.response.CommentResponse createComment(UUID workOrderId, com.zidio.keystone.dto.request.CommentRequest request) {
        WorkOrder wo = getAndCheckAccess(workOrderId);
        KeystoneUserDetails userDetails = SecurityUtils.getCurrentUser();
        User author = userRepository.findById(userDetails.getId()).orElseThrow();

        com.zidio.keystone.domain.Comment comment = new com.zidio.keystone.domain.Comment();
        comment.setWorkOrder(wo);
        comment.setAuthor(author);
        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);

        java.util.Set<UUID> targetUserIds = new java.util.HashSet<>();
        
        if (wo.getAssignedTo() != null && !wo.getAssignedTo().getId().equals(author.getId())) {
            targetUserIds.add(wo.getAssignedTo().getId());
        }
        
        if (wo.getCreatedBy() != null && !wo.getCreatedBy().getId().equals(author.getId())) {
            targetUserIds.add(wo.getCreatedBy().getId());
        }

        for (UUID targetId : targetUserIds) {
            com.zidio.keystone.domain.Notification notif = new com.zidio.keystone.domain.Notification();
            notif.setUser(userRepository.getReferenceById(targetId));
            notif.setWorkOrder(wo);
            notif.setType(com.zidio.keystone.domain.NotificationType.NEW_COMMENT);
            notif.setMessage("New comment on work order " + wo.getCode() + " by " + author.getName());
            notificationRepository.save(notif);
        }

        return com.zidio.keystone.dto.response.CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(author.getId())
                .authorName(author.getName())
                .authorRole(userDetails.getRole())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public com.zidio.keystone.dto.response.PageResponse<com.zidio.keystone.dto.response.CommentResponse> getComments(UUID workOrderId, org.springframework.data.domain.Pageable pageable) {
        getAndCheckAccess(workOrderId);
        org.springframework.data.domain.Page<com.zidio.keystone.domain.Comment> page = commentRepository.findByWorkOrderIdOrderByCreatedAtAsc(workOrderId, pageable);
        
        List<com.zidio.keystone.dto.response.CommentResponse> content = page.getContent().stream()
                .map(c -> com.zidio.keystone.dto.response.CommentResponse.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .authorId(c.getAuthor().getId())
                        .authorName(c.getAuthor().getName())
                        .authorRole(c.getAuthor().getRole())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
                
        return new com.zidio.keystone.dto.response.PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    private WorkOrder getAndCheckAccess(UUID id) {
        WorkOrder wo = workOrderRepository.findByIdWithRelations(id)
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
