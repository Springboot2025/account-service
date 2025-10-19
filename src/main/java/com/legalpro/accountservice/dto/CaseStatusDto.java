package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseStatusDto {
    private Long id;
    private String name;
}
