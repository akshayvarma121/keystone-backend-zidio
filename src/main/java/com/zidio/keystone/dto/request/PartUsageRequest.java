package com.zidio.keystone.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class PartUsageRequest {
    @NotNull
    private UUID partId;

    @NotNull
    @Min(1)
    private Integer qtyUsed;
}
