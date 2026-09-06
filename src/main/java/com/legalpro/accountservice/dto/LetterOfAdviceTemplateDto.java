package com.legalpro.accountservice.dto;

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
public class LetterOfAdviceTemplateDto {
    private UUID uuid;
    private String name;
    private String description;
    private String category;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
