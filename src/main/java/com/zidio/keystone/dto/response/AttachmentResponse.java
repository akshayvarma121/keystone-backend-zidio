package com.zidio.keystone.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AttachmentResponse {
    private UUID id;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private UUID uploadedById;
    private String uploadedByName;
    private OffsetDateTime uploadedAt;
}
