package com.zidio.keystone.dto.response;

import com.zidio.keystone.domain.WorkOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkOrderStatusHistoryResponse {
    private UUID id;
    private WorkOrderStatus fromStatus;
    private WorkOrderStatus toStatus;
    private UUID changedById;
    private String changedByName;
    private String note;
    private OffsetDateTime changedAt;
}
