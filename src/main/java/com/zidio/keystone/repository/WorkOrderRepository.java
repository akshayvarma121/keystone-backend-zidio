package com.zidio.keystone.repository;

import com.zidio.keystone.domain.Priority;
import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    @Query(value = "SELECT nextval('work_order_seq')", nativeQuery = true)
    Long getNextSequence();

    @Query("SELECT w FROM WorkOrder w WHERE " +
           "(:title IS NULL OR LOWER(w.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%'))) AND " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:priority IS NULL OR w.priority = :priority) AND " +
           "(:assignedTo IS NULL OR w.assignedTo.id = :assignedTo) AND " +
           "(:customerId IS NULL OR w.customer.id = :customerId) AND " +
           "(:siteId IS NULL OR w.site.id = :siteId)")
    Page<WorkOrder> searchWorkOrders(
            @Param("title") String title,
            @Param("status") WorkOrderStatus status,
            @Param("priority") Priority priority,
            @Param("assignedTo") UUID assignedTo,
            @Param("customerId") UUID customerId,
            @Param("siteId") UUID siteId,
            Pageable pageable);

    @Query("SELECT w FROM WorkOrder w WHERE " +
           "w.status NOT IN ('CLOSED', 'CANCELLED') AND " +
           "(:assignedTo IS NULL OR w.assignedTo.id = :assignedTo) AND " +
           "(:customerId IS NULL OR w.customer.id = :customerId)")
    List<WorkOrder> getBoard(@Param("assignedTo") UUID assignedTo, @Param("customerId") UUID customerId);
}
