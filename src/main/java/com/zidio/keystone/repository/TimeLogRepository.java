package com.zidio.keystone.repository;

import com.zidio.keystone.domain.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {
    List<TimeLog> findByWorkOrderIdOrderByLoggedAtDesc(UUID workOrderId);
}
