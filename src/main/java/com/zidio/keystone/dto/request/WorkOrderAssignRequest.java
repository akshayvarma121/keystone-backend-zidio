package com.zidio.keystone.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class WorkOrderAssignRequest {
    @NotNull
    private UUID assigneeId;
    private String note;
}
