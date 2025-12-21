package com.legalpro.accountservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProfessionalMaterialUploadRequestDto {

    private UUID caseUuid;

    private Long documentCatId;

    private String followUp;

    private String description;
}
