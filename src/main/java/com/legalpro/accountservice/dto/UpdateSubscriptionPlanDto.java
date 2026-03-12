package com.legalpro.accountservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSubscriptionPlanDto {

    private String planName;

    private String description;

    private BigDecimal monthlyPrice;

    private BigDecimal annualPrice;

    private Boolean recommended;

    private JsonNode features;
}