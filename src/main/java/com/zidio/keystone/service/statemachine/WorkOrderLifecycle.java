package com.zidio.keystone.service.statemachine;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.domain.WorkOrderStatusHistory;
import com.zidio.keystone.dto.response.WorkOrderResponse;
import com.zidio.keystone.exception.IllegalTransitionException;
import com.zidio.keystone.mapper.WorkOrderMapper;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.repository.WorkOrderRepository;
import com.zidio.keystone.repository.WorkOrderStatusHistoryRepository;
import com.zidio.keystone.security.KeystoneUserDetails;
import com.zidio.keystone.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.zidio.keystone.domain.WorkOrderStatus.*;

@Service
@RequiredArgsConstructor
public class WorkOrderLifecycle {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED = Map.of(
            NEW, Set.of(ASSIGNED, CANCELLED),
            ASSIGNED, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(ON_HOLD, COMPLETED),
            ON_HOLD, Set.of(IN_PROGRESS),
            COMPLETED, Set.of(CLOSED),
            CLOSED, Set.of(),
            CANCELLED, Set.of()
    );

    @Transactional
    public WorkOrderResponse assign(UUID workOrderId, UUID assigneeId, String note) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        if (user.getRole() != Role.DISPATCHER && user.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Only dispatchers or managers can assign work orders");
        }

        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new EntityNotFoundException("WorkOrder not found"));

        if (wo.getStatus() != NEW && wo.getStatus() != ASSIGNED) {
            throw new IllegalTransitionException("Can only assign work orders in NEW or ASSIGNED status. Current: " + wo.getStatus());
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new EntityNotFoundException("Assignee not found"));

        if (assignee.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("Can only assign to technicians");
        }

        WorkOrderStatus fromStatus = wo.getStatus();
        wo.setAssignedTo(assignee);
        wo.setStatus(ASSIGNED);
        wo = workOrderRepository.save(wo);

        writeHistory(wo, fromStatus, ASSIGNED, user.getId(), note);

        return WorkOrderMapper.toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse transition(UUID workOrderId, WorkOrderStatus targetStatus, String note) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new EntityNotFoundException("WorkOrder not found"));

        WorkOrderStatus currentStatus = wo.getStatus();
        if (!ALLOWED.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new IllegalTransitionException(
                    String.format("Cannot move work order from %s to %s", currentStatus, targetStatus));
        }

        checkRoleGates(wo, targetStatus, user);

        wo.setStatus(targetStatus);
        wo = workOrderRepository.save(wo);

        writeHistory(wo, currentStatus, targetStatus, user.getId(), note);

        return WorkOrderMapper.toResponse(wo);
    }

    private void checkRoleGates(WorkOrder wo, WorkOrderStatus targetStatus, KeystoneUserDetails user) {
        Role role = user.getRole();

        if (targetStatus == CANCELLED) {
            if (role != Role.DISPATCHER && role != Role.MANAGER) {
                throw new AccessDeniedException("Only dispatchers or managers can cancel work orders");
            }
        } else if (targetStatus == CLOSED) {
            if (role != Role.MANAGER) {
                throw new AccessDeniedException("Only managers can close work orders");
            }
        } else if (Set.of(IN_PROGRESS, ON_HOLD, COMPLETED).contains(targetStatus)) {
            if (role != Role.TECHNICIAN) {
                throw new AccessDeniedException("Only technicians can start, hold, or complete work orders");
            }
            if (wo.getAssignedTo() == null || !wo.getAssignedTo().getId().equals(user.getId())) {
                throw new AccessDeniedException("Cannot transition a work order not assigned to you");
            }
        }
    }

    private void writeHistory(WorkOrder wo, WorkOrderStatus fromStatus, WorkOrderStatus toStatus, UUID userId, String note) {
        User actingUser = userRepository.getReferenceById(userId);
        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setWorkOrder(wo);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(actingUser);
        history.setNote(note);
        historyRepository.save(history);
    }
}
