package com.zidio.keystone.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimeLogRequest {
    @NotNull
    @Min(1)
    private Integer minutes;

    private String note;
}
