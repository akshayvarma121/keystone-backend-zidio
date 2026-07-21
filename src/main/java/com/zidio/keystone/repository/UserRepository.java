package com.zidio.keystone.repository;

import com.zidio.keystone.domain.User;
import com.zidio.keystone.dto.response.AssignmentCandidateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u.id AS technicianId, u.name AS technicianName, " +
           "(CASE WHEN :skillId IS NOT NULL AND EXISTS(SELECT 1 FROM UserSkill us WHERE us.user.id = u.id AND us.skill.id = :skillId) THEN true ELSE false END) AS hasRequiredSkill, " +
           "(SELECT count(w) FROM WorkOrder w WHERE w.assignedTo.id = u.id AND w.status NOT IN ('CLOSED', 'CANCELLED')) AS openJobsCount " +
           "FROM User u " +
           "WHERE u.role = 'TECHNICIAN' " +
           "ORDER BY hasRequiredSkill DESC, openJobsCount ASC")
    List<AssignmentCandidateProjection> getAssignmentCandidates(@Param("skillId") UUID skillId);
}
