package com.zidio.keystone.mapper;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatusHistory;
import com.zidio.keystone.dto.response.WorkOrderResponse;
import com.zidio.keystone.dto.response.WorkOrderStatusHistoryResponse;

import com.zidio.keystone.domain.Role;

public class WorkOrderMapper {

    public static WorkOrderResponse toResponse(WorkOrder wo, Role role) {
        if (wo == null) return null;
        WorkOrderResponse response = WorkOrderResponse.builder()
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
                .requiredSkillId(wo.getRequiredSkill() != null ? wo.getRequiredSkill().getId() : null)
                .requiredSkillName(wo.getRequiredSkill() != null ? wo.getRequiredSkill().getName() : null)
                .slaDueAt(wo.getSlaDueAt())
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
                
        if (role != Role.CUSTOMER) {
            response.setAssignedTo(wo.getAssignedTo() != null ? wo.getAssignedTo().getId() : null);
            response.setAssignedToName(wo.getAssignedTo() != null ? wo.getAssignedTo().getName() : null);
        }
        
        return response;
    }

    public static WorkOrderStatusHistoryResponse toHistoryResponse(WorkOrderStatusHistory h, Role role) {
        if (h == null) return null;
        WorkOrderStatusHistoryResponse response = WorkOrderStatusHistoryResponse.builder()
                .id(h.getId())
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getName() : null)
                .changedAt(h.getChangedAt())
                .build();
                
        if (role != Role.CUSTOMER) {
            response.setNote(h.getNote());
        }
        
        return response;
    }
}
