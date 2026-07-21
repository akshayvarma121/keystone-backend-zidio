package com.zidio.keystone.repository;

import com.zidio.keystone.domain.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer", "site", "requiredSkill", "assignedTo"})
    List<MaintenanceSchedule> findByActiveTrueAndNextRunAtBefore(OffsetDateTime now);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer", "site", "requiredSkill", "assignedTo"})
    @org.springframework.lang.NonNull
    List<MaintenanceSchedule> findAll();
}
