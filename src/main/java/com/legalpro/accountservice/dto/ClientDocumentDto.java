package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ClientDocumentDto {
    private Long id;
    private UUID clientUuid;
    private UUID lawyerUuid;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
