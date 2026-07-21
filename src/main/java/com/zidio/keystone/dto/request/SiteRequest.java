package com.zidio.keystone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SiteRequest {
    @NotNull
    private UUID customerId;

    @NotBlank
    private String name;

    private String address;
}
