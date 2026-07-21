package com.zidio.keystone.dto.response;

import com.zidio.keystone.domain.WorkOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReportSummaryResponse {
    private Map<WorkOrderStatus, Long> countsByStatus;
    private long overdueCount;
    private double slaCompliancePercentage;
    private Map<String, Long> breakdownByTechnician;
    private Map<String, Long> breakdownBySite;
    private List<PartLowStockDto> lowStockParts;
    private Double averageSatisfactionRating;
    private Long satisfactionRatingCount;
}
