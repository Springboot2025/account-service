package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentTemplateCenterDto {

    private Long id;              // internal reference
    private UUID uuid;            // business identifier
    private Long subheadingId;    // grouping
    private String fileName;
    private String fileType;
    private String fileUrl;

    private LocalDateTime createdAt;
}
