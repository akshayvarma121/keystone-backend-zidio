package com.zidio.keystone;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.exception.IllegalTransitionException;
import com.zidio.keystone.repository.*;
import com.zidio.keystone.security.KeystoneUserDetails;
import com.zidio.keystone.service.statemachine.WorkOrderLifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class WorkOrderLifecycleTest {

    @Autowired private WorkOrderLifecycle workOrderLifecycle;
    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private WorkOrderStatusHistoryRepository historyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SiteRepository siteRepository;

    private User manager;
    private User tech1;
    private User tech2;
    private WorkOrder workOrder;

    @BeforeEach
    void setup() {
        historyRepository.deleteAll();
        workOrderRepository.deleteAll();
        siteRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        manager = createUser("manager", Role.MANAGER);
        tech1 = createUser("tech1", Role.TECHNICIAN);
        tech2 = createUser("tech2", Role.TECHNICIAN);

        Customer c = new Customer();
        c.setName("C");
        c = customerRepository.save(c);

        Site s = new Site();
        s.setCustomer(c);
        s.setName("S");
        s = siteRepository.save(s);

        WorkOrder wo = new WorkOrder();
        wo.setCode("WO-1");
        wo.setTitle("Test WO");
        wo.setPriority(Priority.HIGH);
        wo.setStatus(WorkOrderStatus.NEW);
        wo.setCustomer(c);
        wo.setSite(s);
        workOrder = workOrderRepository.save(wo);
    }

    private User createUser(String name, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(name + "@t.com");
        u.setPasswordHash("h");
        u.setRole(role);
        return userRepository.save(u);
    }

    private void mockAuth(User u) {
        KeystoneUserDetails details = new KeystoneUserDetails(u.getId(), u.getEmail(), u.getPasswordHash(), u.getRole(), null, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    void testIllegalJumpRejected() {
        mockAuth(manager);
        assertThrows(IllegalTransitionException.class, () -> {
            workOrderLifecycle.transition(workOrder.getId(), WorkOrderStatus.COMPLETED, "note");
        });
    }

    @Test
    void testLegalJumpWritesHistory() {
        mockAuth(manager);
        workOrderLifecycle.assign(workOrder.getId(), tech1.getId(), "assigning");
        
        mockAuth(tech1);
        workOrderLifecycle.transition(workOrder.getId(), WorkOrderStatus.IN_PROGRESS, "starting");
        
        List<WorkOrderStatusHistory> history = historyRepository.findByWorkOrderIdOrderByChangedAtDesc(workOrder.getId());
        assertEquals(2, history.size());
        assertEquals(WorkOrderStatus.IN_PROGRESS, history.get(0).getToStatus());
        assertEquals(WorkOrderStatus.ASSIGNED, history.get(1).getToStatus());
    }

    @Test
    void testTechnicianCannotStartUnassignedJob() {
        mockAuth(manager);
        workOrderLifecycle.assign(workOrder.getId(), tech1.getId(), "assigning");

        mockAuth(tech2);
        assertThrows(AccessDeniedException.class, () -> {
            workOrderLifecycle.transition(workOrder.getId(), WorkOrderStatus.IN_PROGRESS, "im poster");
        });
    }

    @Test
    void testConcurrentTransitions() throws InterruptedException {
        mockAuth(manager);
        workOrderLifecycle.assign(workOrder.getId(), tech1.getId(), "assigning");

        int threads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    mockAuth(tech1);
                    workOrderLifecycle.transition(workOrder.getId(), WorkOrderStatus.IN_PROGRESS, "starting concurrent");
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown();
        doneLatch.await();

        assertEquals(1, successCount.get(), "Exactly one should succeed");
        assertEquals(1, failCount.get(), "Exactly one should fail due to optimistic locking");
    }
}
