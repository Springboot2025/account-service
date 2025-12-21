package com.legalpro.accountservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CaseDto {

    private Long id;
    private UUID uuid;
    private String caseNumber;
    private String listing;
    private String name;
    private String caseTypeName;
}
