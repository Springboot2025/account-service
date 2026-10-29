package com.legalpro.accountservice.dto.payway;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UpdateSeatsResponse {
    private Integer additionalSeats;   // new total, now in effect
    private Integer usedSeats;         // lawyers actually invited/joined
    private BigDecimal newMonthlyAmount;  // plan price + seats, excl. GST -- what renews from now on
    private BigDecimal proRataCharge;     // charged right now for the remainder of this cycle (0 when removing seats)
    private String invoiceNumber;         // set when a pro-rata charge was made
    private String message;
}
