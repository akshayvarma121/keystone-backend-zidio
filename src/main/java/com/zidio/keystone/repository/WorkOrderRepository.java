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

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer", "site", "assignedTo", "requiredSkill"})
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

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer", "site", "assignedTo", "requiredSkill"})
    @Query("SELECT w FROM WorkOrder w WHERE " +
           "w.status NOT IN ('CLOSED', 'CANCELLED') AND " +
           "(:assignedTo IS NULL OR w.assignedTo.id = :assignedTo) AND " +
           "(:customerId IS NULL OR w.customer.id = :customerId)")
    List<WorkOrder> getBoard(@Param("assignedTo") UUID assignedTo, @Param("customerId") UUID customerId);

    @Query(value = """
        SELECT cast(w.id as varchar) FROM work_orders w 
        WHERE w.search_vector @@ plainto_tsquery('english', :q)
        AND (
            :role = 'MANAGER' OR :role = 'DISPATCHER'
            OR (:role = 'CUSTOMER' AND w.customer_id = CAST(:customerId AS UUID))
            OR (:role = 'TECHNICIAN' AND w.assigned_to = CAST(:userId AS UUID))
        )
        ORDER BY ts_rank(w.search_vector, plainto_tsquery('english', :q)) DESC
    """, countQuery = """
        SELECT count(*) FROM work_orders w 
        WHERE w.search_vector @@ plainto_tsquery('english', :q)
        AND (
            :role = 'MANAGER' OR :role = 'DISPATCHER'
            OR (:role = 'CUSTOMER' AND w.customer_id = CAST(:customerId AS UUID))
            OR (:role = 'TECHNICIAN' AND w.assigned_to = CAST(:userId AS UUID))
        )
    """, nativeQuery = true)
    Page<String> fullTextSearchIds(
            @Param("q") String q, 
            @Param("role") String role, 
            @Param("customerId") String customerId, 
            @Param("userId") String userId, 
            Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer", "site", "assignedTo", "requiredSkill"})
    @Query("SELECT w FROM WorkOrder w WHERE w.id IN :ids")
    List<WorkOrder> findByIdInWithRelations(@Param("ids") List<UUID> ids);

    @Query("SELECT w.status as status, COUNT(w) as count FROM WorkOrder w WHERE " +
           "(:startDate IS NULL OR w.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR w.createdAt <= :endDate) AND " +
           "(:siteId IS NULL OR w.site.id = :siteId) AND " +
           "(:technicianId IS NULL OR w.assignedTo.id = :technicianId) " +
           "GROUP BY w.status")
    List<com.zidio.keystone.repository.projection.StatusCountProjection> getStatusCounts(
            @Param("startDate") java.time.OffsetDateTime startDate,
            @Param("endDate") java.time.OffsetDateTime endDate,
            @Param("siteId") UUID siteId,
            @Param("technicianId") UUID technicianId);

    @Query("SELECT COUNT(w) FROM WorkOrder w WHERE " +
           "w.status NOT IN ('COMPLETED', 'CLOSED', 'CANCELLED') AND " +
           "w.slaDueAt < CURRENT_TIMESTAMP AND " +
           "(:startDate IS NULL OR w.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR w.createdAt <= :endDate) AND " +
           "(:siteId IS NULL OR w.site.id = :siteId) AND " +
           "(:technicianId IS NULL OR w.assignedTo.id = :technicianId)")
    long getOverdueCount(
            @Param("startDate") java.time.OffsetDateTime startDate,
            @Param("endDate") java.time.OffsetDateTime endDate,
            @Param("siteId") UUID siteId,
            @Param("technicianId") UUID technicianId);

    @Query("SELECT COUNT(w) as total, SUM(CASE WHEN w.slaDueAt < w.updatedAt THEN 1 ELSE 0 END) as breached FROM WorkOrder w WHERE " +
           "w.status IN ('COMPLETED', 'CLOSED') AND " +
           "(:startDate IS NULL OR w.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR w.createdAt <= :endDate) AND " +
           "(:siteId IS NULL OR w.site.id = :siteId) AND " +
           "(:technicianId IS NULL OR w.assignedTo.id = :technicianId)")
    com.zidio.keystone.repository.projection.SlaComplianceProjection getSlaComplianceStats(
            @Param("startDate") java.time.OffsetDateTime startDate,
            @Param("endDate") java.time.OffsetDateTime endDate,
            @Param("siteId") UUID siteId,
            @Param("technicianId") UUID technicianId);

    @Query("SELECT w.assignedTo.name as label, COUNT(w) as count FROM WorkOrder w WHERE " +
           "w.assignedTo IS NOT NULL AND " +
           "(:startDate IS NULL OR w.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR w.createdAt <= :endDate) AND " +
           "(:siteId IS NULL OR w.site.id = :siteId) AND " +
           "(:technicianId IS NULL OR w.assignedTo.id = :technicianId) " +
           "GROUP BY w.assignedTo.name")
    List<com.zidio.keystone.repository.projection.StringCountProjection> getBreakdownByTechnician(
            @Param("startDate") java.time.OffsetDateTime startDate,
            @Param("endDate") java.time.OffsetDateTime endDate,
            @Param("siteId") UUID siteId,
            @Param("technicianId") UUID technicianId);

    @Query("SELECT w.site.name as label, COUNT(w) as count FROM WorkOrder w WHERE " +
           "(:startDate IS NULL OR w.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR w.createdAt <= :endDate) AND " +
           "(:siteId IS NULL OR w.site.id = :siteId) AND " +
           "(:technicianId IS NULL OR w.assignedTo.id = :technicianId) " +
           "GROUP BY w.site.name")
    List<com.zidio.keystone.repository.projection.StringCountProjection> getBreakdownBySite(
            @Param("startDate") java.time.OffsetDateTime startDate,
            @Param("endDate") java.time.OffsetDateTime endDate,
            @Param("siteId") UUID siteId,
            @Param("technicianId") UUID technicianId);

    @Query("SELECT AVG(w.satisfactionRating) as average, COUNT(w.satisfactionRating) as count FROM WorkOrder w WHERE " +
           "w.satisfactionRating IS NOT NULL AND " +
           "(:startDate IS NULL OR w.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR w.createdAt <= :endDate) AND " +
           "(:siteId IS NULL OR w.site.id = :siteId) AND " +
           "(:technicianId IS NULL OR w.assignedTo.id = :technicianId)")
    com.zidio.keystone.repository.projection.SatisfactionProjection getSatisfactionStats(
            @Param("startDate") java.time.OffsetDateTime startDate,
            @Param("endDate") java.time.OffsetDateTime endDate,
            @Param("siteId") UUID siteId,
            @Param("technicianId") UUID technicianId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer", "site", "assignedTo", "requiredSkill"})
    @Query("SELECT w FROM WorkOrder w WHERE w.id = :id")
    java.util.Optional<WorkOrder> findByIdWithRelations(@Param("id") UUID id);
}
