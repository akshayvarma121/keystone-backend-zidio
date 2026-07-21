package com.zidio.keystone.mapper;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatusHistory;
import com.zidio.keystone.dto.response.WorkOrderResponse;
import com.zidio.keystone.dto.response.WorkOrderStatusHistoryResponse;

public class WorkOrderMapper {

    public static WorkOrderResponse toResponse(WorkOrder wo) {
        if (wo == null) return null;
        return WorkOrderResponse.builder()
                .id(wo.getId())
                .code(wo.getCode())
                .title(wo.getTitle())
                .description(wo.getDescription())
                .priority(wo.getPriority())
                .status(wo.getStatus())
                .customerId(wo.getCustomer() != null ? wo.getCustomer().getId() : null)
                .customerName(wo.getCustomer() != null ? wo.getCustomer().getName() : null)
                .siteId(wo.getSite() != null ? wo.getSite().getId() : null)
                .siteName(wo.getSite() != null ? wo.getSite().getName() : null)
                .assignedTo(wo.getAssignedTo() != null ? wo.getAssignedTo().getId() : null)
                .assignedToName(wo.getAssignedTo() != null ? wo.getAssignedTo().getName() : null)
                .requiredSkillId(wo.getRequiredSkill() != null ? wo.getRequiredSkill().getId() : null)
                .requiredSkillName(wo.getRequiredSkill() != null ? wo.getRequiredSkill().getName() : null)
                .slaDueAt(wo.getSlaDueAt())
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
    }

    public static WorkOrderStatusHistoryResponse toHistoryResponse(WorkOrderStatusHistory h) {
        if (h == null) return null;
        return WorkOrderStatusHistoryResponse.builder()
                .id(h.getId())
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getName() : null)
                .note(h.getNote())
                .changedAt(h.getChangedAt())
                .build();
    }
}
