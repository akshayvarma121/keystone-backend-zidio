package com.zidio.keystone.repository;

import com.zidio.keystone.domain.PartUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PartUsageRepository extends JpaRepository<PartUsage, UUID> {
    List<PartUsage> findByWorkOrderIdOrderByLoggedAtDesc(UUID workOrderId);
}
