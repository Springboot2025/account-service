package com.legalpro.accountservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTemplateCenterSummaryDto {

    private long totalDocuments;
    private long documentsThisMonth;
    private long sharedDocuments;
}
