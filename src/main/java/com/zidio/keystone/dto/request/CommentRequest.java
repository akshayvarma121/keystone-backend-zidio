package com.zidio.keystone.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "Content cannot be blank")
    private String content;
}
