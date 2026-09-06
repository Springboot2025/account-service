package com.legalpro.accountservice.dto;

import com.legalpro.accountservice.enums.LetterOfAdviceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfAdviceDocumentDto {
    private UUID uuid;
    private UUID lawyerUuid;
    private UUID clientUuid;
    private UUID caseUuid;
    private UUID templateUuid;
    private String title;
    private String content;
    private LetterOfAdviceStatus status;
    private String lawyerSignature;
    private LocalDateTime lawyerSignedAt;
    private String clientSignature;
    private LocalDateTime clientSignedAt;
    private LocalDateTime sentToClientAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
