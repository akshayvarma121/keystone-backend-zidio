package com.zidio.keystone.dto.request;

import com.zidio.keystone.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkOrderTransitionRequest {
    @NotNull
    private WorkOrderStatus targetStatus;
    private String note;
}
