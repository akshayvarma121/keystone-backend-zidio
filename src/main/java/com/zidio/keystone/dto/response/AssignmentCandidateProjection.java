package com.zidio.keystone.dto.response;

import java.util.UUID;

public interface AssignmentCandidateProjection {
    UUID getTechnicianId();
    String getTechnicianName();
    boolean getHasRequiredSkill();
    long getOpenJobsCount();
}
