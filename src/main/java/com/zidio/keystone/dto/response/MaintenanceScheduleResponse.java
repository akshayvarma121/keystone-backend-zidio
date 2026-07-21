package com.zidio.keystone.dto.response;

import com.zidio.keystone.domain.Priority;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MaintenanceScheduleResponse {
    private UUID id;
    private String title;
    private String description;
    private Priority priority;
    private UUID customerId;
    private String customerName;
    private UUID siteId;
    private String siteName;
    private UUID requiredSkillId;
    private String requiredSkillName;
    private Integer frequencyDays;
    private OffsetDateTime nextRunAt;
    private Boolean active;
    private UUID createdById;
    private OffsetDateTime createdAt;
}
