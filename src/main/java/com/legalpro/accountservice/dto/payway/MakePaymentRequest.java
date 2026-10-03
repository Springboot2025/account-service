package com.legalpro.accountservice.dto.payway;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MakePaymentRequest {

    @NotNull
    private Long planId;

    @NotBlank
    @Pattern(regexp = "(?i)monthly|yearly", message = "must be monthly or yearly")
    private String planDuration;

    // Plan price excl. GST -- must match the plan's price in the DB
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal gstAmount;

    // amount + gstAmount; this is what gets charged
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal totalAmount;

    // From payway.js / POST /single-use-tokens -- single use, expires after 10 minutes
    @NotBlank
    private String singleUseTokenId;
}
