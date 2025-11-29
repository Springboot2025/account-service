package com.legalpro.accountservice.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientFullResponseDto {

    private ClientDto profileData;

    private List<ClientAnswerDto> questions;

    private List<CourtSupportMaterialDto> courtSupportingMaterial;

    private List<ClientDocumentDto> documents;
    private List<QuoteDto> quotes;
}
