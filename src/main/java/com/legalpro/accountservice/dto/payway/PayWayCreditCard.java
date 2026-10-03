package com.legalpro.accountservice.dto.payway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Card details as PayWay returns them -- the number is already masked (456471...004)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayWayCreditCard {
    private String cardNumber;
    private String expiryDateMonth;
    private String expiryDateYear;
    private String cardScheme;
    private String cardholderName;
}
