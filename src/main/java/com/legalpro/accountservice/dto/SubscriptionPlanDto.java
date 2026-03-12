package com.legalpro.accountservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanDto {

    private UUID uuid;

    private String planName;

    private String description;

    private BigDecimal monthlyPrice;

    private BigDecimal annualPrice;

    private Boolean recommended;

    private JsonNode features;
}