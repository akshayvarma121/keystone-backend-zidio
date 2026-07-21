package com.zidio.keystone.dto.response;

import com.zidio.keystone.domain.Role;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private String content;
    private UUID authorId;
    private String authorName;
    private Role authorRole;
    private OffsetDateTime createdAt;
}
