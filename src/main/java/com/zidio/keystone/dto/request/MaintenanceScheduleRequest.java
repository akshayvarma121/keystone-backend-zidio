package com.zidio.keystone.dto.request;

import com.zidio.keystone.domain.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class MaintenanceScheduleRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private Priority priority;
    @NotNull
    private UUID customerId;
    @NotNull
    private UUID siteId;
    private UUID requiredSkillId;
    
    @NotNull
    @Min(1)
    private Integer frequencyDays;
    
    @NotNull
    private OffsetDateTime nextRunAt;
}
