package com.legalpro.accountservice.dto.payway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

// Response of PayWay POST /transactions and GET /transactions/{id}
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayWayTransactionResponse {
    private Long transactionId;
    private String receiptNumber;
    private String status;              // approved | approved* | declined | pending | ...
    private String responseCode;
    private String responseText;
    private String customerNumber;
    private String orderNumber;
    private BigDecimal principalAmount;
    private BigDecimal surchargeAmount;
    private BigDecimal paymentAmount;
    private String transactionDateTime; // e.g. "30 Sep 2026 11:55 AEST"
    private String settlementDate;      // e.g. "30 Sep 2026"
    private PayWayCreditCard creditCard;

    public boolean isApproved() {
        // "approved*" = approved with conditions, still money in the bank
        return status != null && status.startsWith("approved");
    }

    // 202 from PayWay when the card network is slow: outcome not known yet
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }
}
