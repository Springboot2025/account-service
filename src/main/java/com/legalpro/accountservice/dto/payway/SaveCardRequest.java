package com.legalpro.accountservice.dto.payway;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveCardRequest {

    // From payway.js / POST /single-use-tokens -- single use, expires after 10 minutes
    @NotBlank
    private String singleUseTokenId;
}
