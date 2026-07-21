package com.zidio.keystone.service;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.request.WorkOrderRequest;
import com.zidio.keystone.repository.MaintenanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceScheduleExecutionService {

    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final WorkOrderService workOrderService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSchedule(UUID scheduleId, OffsetDateTime now) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(scheduleId)
                .orElse(null);

        if (schedule == null || !schedule.getActive()) {
            return;
        }

        if (schedule.getNextRunAt().isAfter(now)) {
            return;
        }

        log.info("Executing maintenance schedule {} (next_run_at was {})", scheduleId, schedule.getNextRunAt());

        WorkOrderRequest request = new WorkOrderRequest();
        request.setTitle(schedule.getTitle() + " (Scheduled Maintenance)");
        request.setDescription(schedule.getDescription());
        request.setPriority(schedule.getPriority());
        request.setCustomerId(schedule.getCustomer().getId());
        request.setSiteId(schedule.getSite().getId());
        if (schedule.getRequiredSkill() != null) {
            request.setRequiredSkillId(schedule.getRequiredSkill().getId());
        }

        workOrderService.createScheduledWorkOrder(request, schedule.getCreatedBy());

        schedule.setNextRunAt(schedule.getNextRunAt().plusDays(schedule.getFrequencyDays()));
        
        while (schedule.getNextRunAt().isBefore(now)) {
            schedule.setNextRunAt(schedule.getNextRunAt().plusDays(schedule.getFrequencyDays()));
        }
        
        maintenanceScheduleRepository.save(schedule);
    }
}
