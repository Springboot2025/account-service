package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryWithSubheadingsAndDocumentsDto {

    private Long categoryId;
    private String categoryKey;
    private String categoryName;
    private Integer displayOrder;

    private List<SubheadingWithDocumentsDto> subheadings;
}
