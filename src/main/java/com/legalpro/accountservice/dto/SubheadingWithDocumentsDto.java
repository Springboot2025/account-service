package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SubheadingWithDocumentsDto {

    private Long subheadingId;
    private UUID subheadingUuid;
    private String subheadingName;

    private List<DocumentTemplateCenterDto> documents;
}
