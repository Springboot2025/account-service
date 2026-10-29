package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CompanyDto {
    private Long id;
    private UUID uuid;
    private String name;
    private String description;
    private String abn;
    private String businessPhone;
    private String businessAddress;
    private String primaryContactName;
    private String primaryContactEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime removedAt;
}
