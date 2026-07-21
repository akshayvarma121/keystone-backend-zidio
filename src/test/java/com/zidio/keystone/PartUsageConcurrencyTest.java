package com.zidio.keystone;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.request.PartUsageRequest;
import com.zidio.keystone.repository.*;
import com.zidio.keystone.security.KeystoneUserDetails;
import com.zidio.keystone.service.WorkOrderService;
import com.zidio.keystone.service.statemachine.WorkOrderLifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import com.zidio.keystone.exception.InsufficientStockException;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class PartUsageConcurrencyTest {

    @Autowired private WorkOrderService workOrderService;
    @Autowired private WorkOrderLifecycle workOrderLifecycle;
    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SiteRepository siteRepository;
    @Autowired private PartRepository partRepository;
    @Autowired private PartUsageRepository partUsageRepository;

    private User manager;
    private User tech1;
    private WorkOrder workOrder;
    private Part part;

    @BeforeEach
    void setup() {
        partUsageRepository.deleteAll();
        partRepository.deleteAll();
        workOrderRepository.deleteAll();
        siteRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        manager = createUser("manager", Role.MANAGER);
        tech1 = createUser("tech1", Role.TECHNICIAN);

        Customer c = new Customer();
        c.setName("C");
        c = customerRepository.save(c);

        Site s = new Site();
        s.setCustomer(c);
        s.setName("S");
        s = siteRepository.save(s);

        WorkOrder wo = new WorkOrder();
        wo.setCode("WO-P1");
        wo.setTitle("Test WO Parts");
        wo.setPriority(Priority.HIGH);
        wo.setStatus(WorkOrderStatus.NEW);
        wo.setCustomer(c);
        wo.setSite(s);
        workOrder = workOrderRepository.save(wo);

        Part p = new Part();
        p.setName("Widget");
        p.setSku("W-01");
        p.setUnitCost(new BigDecimal("10.00"));
        p.setStockQty(5);
        part = partRepository.save(p);
    }

    private User createUser(String name, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(name + "@t.com");
        u.setPasswordHash("h");
        u.setRole(role);
        u.setHourlyRate(new BigDecimal("50.00"));
        return userRepository.save(u);
    }

    private void mockAuth(User u) {
        KeystoneUserDetails details = new KeystoneUserDetails(u.getId(), u.getEmail(), u.getPasswordHash(), u.getRole(), null, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    void testConcurrentPartUsage() throws InterruptedException {
        // assign WO to tech
        mockAuth(manager);
        workOrderLifecycle.assign(workOrder.getId(), tech1.getId(), "assigning");

        int threads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger insufficientCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    mockAuth(tech1);
                    PartUsageRequest req = new PartUsageRequest();
                    req.setPartId(part.getId());
                    req.setQtyUsed(4); // 4 * 2 = 8 > 5 (stock)
                    workOrderService.logPartUsage(workOrder.getId(), req);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    failCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    insufficientCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown();
        doneLatch.await();

        assertEquals(1, successCount.get(), "Exactly one request should succeed");
        // The other fails either by optimistic lock or explicitly catching insufficient stock
        assertEquals(1, failCount.get() + insufficientCount.get(), "Exactly one should fail");

        Part updatedPart = partRepository.findById(part.getId()).get();
        assertEquals(1, updatedPart.getStockQty(), "Stock should be reduced by exactly 4");
    }
}
