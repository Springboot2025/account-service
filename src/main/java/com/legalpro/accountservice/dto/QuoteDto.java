package com.legalpro.accountservice.dto;

import com.legalpro.accountservice.enums.QuoteStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDto {

    private Long id;
    private UUID uuid;

    private UUID lawyerUuid;
    private UUID clientUuid;

    private String title;
    private String description;

    private BigDecimal expectedAmount;
    private BigDecimal quotedAmount;
    private String currency;
    private List<String> offenceList;

    private QuoteStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
