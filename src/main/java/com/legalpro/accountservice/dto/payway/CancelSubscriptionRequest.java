package com.legalpro.accountservice.dto.payway;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelSubscriptionRequest {

    // Optional: "Tell us why you're leaving"
    @Size(max = 1000)
    private String reason;
}
