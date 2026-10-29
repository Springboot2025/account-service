package com.legalpro.accountservice.dto.payway;

import lombok.Builder;
import lombok.Data;

// The card now saved on the payer's PayWay customer (masked by PayWay)
@Data
@Builder
public class SavedCardResponse {
    private String customerNumber;
    private String cardScheme;          // visa | mastercard | ...
    private String maskedCardNumber;    // e.g. 516320...008
    private String expiryDateMonth;
    private String expiryDateYear;
    private String cardholderName;
    private Boolean overdueRenewal;     // true = a renewal is OVERDUE; the next nightly retry charges this card
}
