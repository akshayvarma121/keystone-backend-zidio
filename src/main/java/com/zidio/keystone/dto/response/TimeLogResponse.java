package com.zidio.keystone.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class TimeLogResponse {
    private UUID id;
    private UUID technicianId;
    private String technicianName;
    private Integer minutes;
    private String note;
    private OffsetDateTime loggedAt;
}
