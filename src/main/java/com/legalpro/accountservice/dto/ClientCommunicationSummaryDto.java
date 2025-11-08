package com.legalpro.accountservice.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientCommunicationSummaryDto {

    // === Client Info ===
    private UUID clientUuid;
    private String clientName;
    private String clientEmail;

    private UUID lawyerUuid;
    private String lawyerName;
    private String lawyerEmail;

    // === Nested Data ===
    private List<MessageDto> messages;
    private List<QuoteDto> quotes;
    private List<AppointmentDto> appointments;
}
