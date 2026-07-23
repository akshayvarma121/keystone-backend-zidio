package com.zidio.keystone.service;

import com.zidio.keystone.domain.Part;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.dto.response.PartLowStockDto;
import com.zidio.keystone.dto.response.ReportSummaryResponse;
import com.zidio.keystone.repository.PartRepository;
import com.zidio.keystone.repository.WorkOrderRepository;
import com.zidio.keystone.repository.projection.SatisfactionProjection;
import com.zidio.keystone.repository.projection.SlaComplianceProjection;
import com.zidio.keystone.repository.projection.StatusCountProjection;
import com.zidio.keystone.repository.projection.StringCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final WorkOrderRepository workOrderRepository;
    private final PartRepository partRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    @org.springframework.cache.annotation.Cacheable(value = "dashboardSummary", sync = true)
    public ReportSummaryResponse getSummary(OffsetDateTime startDate, OffsetDateTime endDate, UUID siteId, UUID technicianId) {
        
        List<StatusCountProjection> statusCounts = workOrderRepository.getStatusCounts(startDate, endDate, siteId, technicianId);
        Map<WorkOrderStatus, Long> countsByStatus = statusCounts.stream()
                .filter(sc -> sc != null && sc.getStatus() != null)
                .collect(Collectors.toMap(StatusCountProjection::getStatus, StatusCountProjection::getCount, (v1, v2) -> v1));

        long overdueCount = workOrderRepository.getOverdueCount(startDate, endDate, siteId, technicianId);

        SlaComplianceProjection slaProj = workOrderRepository.getSlaComplianceStats(startDate, endDate, siteId, technicianId);
        double compliancePercentage = 100.0;
        if (slaProj != null && slaProj.getTotal() != null && slaProj.getTotal() > 0) {
            long total = slaProj.getTotal();
            long breached = slaProj.getBreached() != null ? slaProj.getBreached() : 0;
            long met = total - breached;
            compliancePercentage = ((double) met / total) * 100.0;
        }

        List<StringCountProjection> techBreakdown = workOrderRepository.getBreakdownByTechnician(startDate, endDate, siteId, technicianId);
        Map<String, Long> breakdownByTechnician = techBreakdown.stream()
                .filter(tb -> tb != null && tb.getLabel() != null)
                .collect(Collectors.toMap(StringCountProjection::getLabel, StringCountProjection::getCount, (v1, v2) -> v1));

        List<StringCountProjection> siteBreakdown = workOrderRepository.getBreakdownBySite(startDate, endDate, siteId, technicianId);
        Map<String, Long> breakdownBySite = siteBreakdown.stream()
                .filter(sb -> sb != null && sb.getLabel() != null)
                .collect(Collectors.toMap(StringCountProjection::getLabel, StringCountProjection::getCount, (v1, v2) -> v1));

        List<Part> parts = partRepository.findLowStockParts();
        List<PartLowStockDto> lowStockParts = parts.stream()
                .map(p -> PartLowStockDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .sku(p.getSku())
                        .stockQty(p.getStockQty())
                        .reorderThreshold(p.getReorderThreshold())
                        .build())
                .collect(Collectors.toList());

        SatisfactionProjection satProj = workOrderRepository.getSatisfactionStats(startDate, endDate, siteId, technicianId);
        Double averageSatisfactionRating = (satProj != null) ? satProj.getAverage() : null;
        Long satisfactionRatingCount = (satProj != null && satProj.getCount() != null) ? satProj.getCount() : 0L;

        return ReportSummaryResponse.builder()
                .countsByStatus(countsByStatus)
                .overdueCount(overdueCount)
                .slaCompliancePercentage(compliancePercentage)
                .breakdownByTechnician(breakdownByTechnician)
                .breakdownBySite(breakdownBySite)
                .lowStockParts(lowStockParts)
                .averageSatisfactionRating(averageSatisfactionRating)
                .satisfactionRatingCount(satisfactionRatingCount)
                .build();
    }
}
