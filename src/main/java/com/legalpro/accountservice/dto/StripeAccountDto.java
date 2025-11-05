package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StripeAccountDto {

    private Long id;
    private UUID uuid;

    private UUID lawyerUuid;
    private String stripeAccountId;

    private boolean chargesEnabled;
    private boolean payoutsEnabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
