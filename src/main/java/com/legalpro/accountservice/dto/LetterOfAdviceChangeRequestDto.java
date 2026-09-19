package com.legalpro.accountservice.dto;

import com.legalpro.accountservice.enums.LetterOfAdviceChangeCategory;
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
public class LetterOfAdviceChangeRequestDto {
    private UUID uuid;
    private UUID documentUuid;
    private LetterOfAdviceChangeCategory category;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
