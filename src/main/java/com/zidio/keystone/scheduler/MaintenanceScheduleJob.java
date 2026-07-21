package com.zidio.keystone.scheduler;

import com.zidio.keystone.domain.MaintenanceSchedule;
import com.zidio.keystone.repository.MaintenanceScheduleRepository;
import com.zidio.keystone.service.MaintenanceScheduleExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceScheduleJob {

    private final MaintenanceScheduleRepository scheduleRepository;
    private final MaintenanceScheduleExecutionService executionService;

    @Scheduled(cron = "0 0 * * * *")
    public void executeDueSchedules() {
        OffsetDateTime now = OffsetDateTime.now();
        List<MaintenanceSchedule> dueSchedules = scheduleRepository.findByActiveTrueAndNextRunAtBefore(now);
        
        if (!dueSchedules.isEmpty()) {
            log.info("Found {} due maintenance schedules", dueSchedules.size());
        }
        
        for (MaintenanceSchedule schedule : dueSchedules) {
            try {
                executionService.processSchedule(schedule.getId(), now);
            } catch (Exception e) {
                log.error("Failed to execute schedule {}", schedule.getId(), e);
            }
        }
    }
}
