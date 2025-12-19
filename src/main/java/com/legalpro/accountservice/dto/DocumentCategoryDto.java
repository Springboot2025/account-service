package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentCategoryDto {

    private Long id;
    private String key;
    private String displayName;
    private Integer displayOrder;
}
