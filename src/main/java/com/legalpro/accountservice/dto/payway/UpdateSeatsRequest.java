package com.legalpro.accountservice.dto.payway;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSeatsRequest {

    // The new total of ADDITIONAL lawyer seats (beyond the firm admin's own), not a delta
    @NotNull
    @Min(0)
    private Integer additionalSeats;
}
