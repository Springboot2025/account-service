package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CaseTypeDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
