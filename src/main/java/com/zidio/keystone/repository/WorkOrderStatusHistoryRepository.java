package com.zidio.keystone.repository;

import com.zidio.keystone.domain.WorkOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkOrderStatusHistoryRepository extends JpaRepository<WorkOrderStatusHistory, UUID> {
    List<WorkOrderStatusHistory> findByWorkOrderIdOrderByChangedAtDesc(UUID workOrderId);
}
