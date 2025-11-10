package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DisputeDocumentDto {

    private Long id;
    private UUID disputeUuid;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private LocalDateTime createdAt;
}
