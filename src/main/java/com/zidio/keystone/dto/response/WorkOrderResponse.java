package com.zidio.keystone.dto.response;

import com.zidio.keystone.domain.Priority;
import com.zidio.keystone.domain.WorkOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkOrderResponse {
    private UUID id;
    private String code;
    private String title;
    private String description;
    private Priority priority;
    private WorkOrderStatus status;
    private UUID customerId;
    private String customerName;
    private UUID siteId;
    private String siteName;
    private UUID assignedTo;
    private String assignedToName;
    private UUID requiredSkillId;
    private String requiredSkillName;
    private OffsetDateTime slaDueAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
