package com.zidio.keystone;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.repository.*;
import com.zidio.keystone.scheduler.MaintenanceScheduleJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
public class MaintenanceScheduleJobTest {

    @Autowired
    private MaintenanceScheduleJob job;

    @Autowired
    private MaintenanceScheduleRepository scheduleRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testScheduleGeneratesExactlyOneWorkOrder() {
        Customer customer = customerRepository.findAll().get(0);
        Site site = siteRepository.findAll().get(0);
        User creator = userRepository.findAll().stream().filter(u -> u.getRole() == Role.MANAGER).findFirst().orElseThrow();

        long initialCount = workOrderRepository.count();

        // 1. Create a schedule due in the past
        OffsetDateTime past = OffsetDateTime.now().minusDays(2);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .title("Monthly HVAC")
                .priority(Priority.LOW)
                .customer(customer)
                .site(site)
                .frequencyDays(30)
                .nextRunAt(past)
                .active(true)
                .createdBy(creator)
                .build();
        schedule = scheduleRepository.save(schedule);

        // 2. Fire the job manually
        job.executeDueSchedules();

        // 3. Verify exactly one work order was created
        long afterFirstRunCount = workOrderRepository.count();
        assertEquals(initialCount + 1, afterFirstRunCount, "Exactly one work order should be created");

        // 4. Verify next_run_at advanced to the future
        MaintenanceSchedule updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertTrue(updatedSchedule.getNextRunAt().isAfter(OffsetDateTime.now()), "Next run at should be advanced to the future");

        // 5. Fire the job a second time
        job.executeDueSchedules();

        // 6. Verify NO duplicate work order was created
        long afterSecondRunCount = workOrderRepository.count();
        assertEquals(afterFirstRunCount, afterSecondRunCount, "No duplicate work orders should be generated if fired again");
        
        // Clean up
        scheduleRepository.delete(updatedSchedule);
    }
}
