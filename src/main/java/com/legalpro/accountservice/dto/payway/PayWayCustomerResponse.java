package com.legalpro.accountservice.dto.payway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Response of PayWay PUT/GET /customers/{customerNumber}
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayWayCustomerResponse {
    private String customerNumber;
    private PayWayPaymentSetup paymentSetup;
}
