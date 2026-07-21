package com.zidio.keystone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class AssignmentCandidateResponse {
    private UUID technicianId;
    private String technicianName;
    private boolean hasRequiredSkill;
    private long openJobsCount;
}
