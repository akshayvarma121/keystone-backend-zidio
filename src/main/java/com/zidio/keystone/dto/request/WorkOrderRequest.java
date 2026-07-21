package com.zidio.keystone.dto.request;

import com.zidio.keystone.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class WorkOrderRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Priority priority;

    @NotNull
    private UUID customerId;

    @NotNull
    private UUID siteId;
    
    private UUID assignedTo;

    private UUID requiredSkillId;
}
