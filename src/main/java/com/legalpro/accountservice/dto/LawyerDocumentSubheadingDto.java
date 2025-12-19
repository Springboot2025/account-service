package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LawyerDocumentSubheadingDto {

    private Long id;           // internal reference (safe to expose)
    private UUID uuid;         // business identifier
    private Long categoryId;   // belongs to which category
    private String name;

    private LocalDateTime createdAt;
}
