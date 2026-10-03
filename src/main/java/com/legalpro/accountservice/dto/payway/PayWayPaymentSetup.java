package com.legalpro.accountservice.dto.payway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayWayPaymentSetup {
    private String paymentMethod;
    private Boolean stopped;
    private PayWayCreditCard creditCard;
}
