package com.legalpro.accountservice.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ShareDocumentRequestDto {

    private List<RecipientDto> recipients;
    private String remarks;

    @Data
    public static class RecipientDto {
        private UUID clientUuid;
        private UUID caseUuid;
    }
}
