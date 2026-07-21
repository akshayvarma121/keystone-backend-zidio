package com.zidio.keystone.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PartLowStockDto {
    private UUID id;
    private String name;
    private String sku;
    private Integer stockQty;
    private Integer reorderThreshold;
}
