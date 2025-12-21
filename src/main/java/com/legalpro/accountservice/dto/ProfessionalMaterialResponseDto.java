package com.legalpro.accountservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ProfessionalMaterialResponseDto {

    private UUID uuid;

    private UUID caseUuid;

    private ProfessionalMaterialCategoryDto category;

    private String followUp;

    private String description;

    private String fileName;

    private String fileType;

    private String fileUrl;

    private LocalDateTime createdAt;
}
