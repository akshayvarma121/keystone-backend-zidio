package com.zidio.keystone.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorkOrderDetailsResponse {
    private WorkOrderResponse workOrder;
    private List<WorkOrderStatusHistoryResponse> history;
    private List<PartUsageResponse> partsUsed;
    private List<TimeLogResponse> timeLogs;
    private java.math.BigDecimal partsCost;
    private Integer totalMinutes;
    private java.math.BigDecimal laborCost;
}
