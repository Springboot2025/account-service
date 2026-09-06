package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfAdviceTemplateRequest {
    private String name;
    private String description;
    private String category;
    private String content;
}
