package com.zidio.keystone.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PartUsageResponse {
    private UUID id;
    private UUID partId;
    private String partName;
    private Integer qtyUsed;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private UUID loggedById;
    private String loggedByName;
    private OffsetDateTime loggedAt;
}
