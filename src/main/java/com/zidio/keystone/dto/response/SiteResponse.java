package com.zidio.keystone.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class SiteResponse {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private String name;
    private String address;
    private OffsetDateTime createdAt;
}
