package com.zidio.keystone.repository;

import com.zidio.keystone.domain.WorkOrderRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkOrderRatingRepository extends JpaRepository<WorkOrderRating, UUID> {
    boolean existsByWorkOrderId(UUID workOrderId);
}
