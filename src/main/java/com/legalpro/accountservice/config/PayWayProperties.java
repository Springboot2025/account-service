package com.legalpro.accountservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * PayWay (Westpac) REST API settings, bound from the "payway" block in
 * application.yml, which reads the PAYWAY_* environment variables.
 * Sandbox and production share the base URL -- the keys and merchant id
 * decide which facility is used.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payway")
public class PayWayProperties {

    private String baseUrl = "https://api.payway.com.au/rest/v1";

    // Server-side only. Used as the HTTP Basic username (blank password).
    private String secretKey;

    // Safe to hand to the browser -- can only create single-use card tokens.
    private String publishableKey;

    // "TEST" for the sandbox facility, the real merchant id in production.
    private String merchantId;

    private String currency = "aud";

    private int connectTimeoutSeconds = 10;

    // PayWay can take a while to get a response from the card schemes.
    private int readTimeoutSeconds = 60;

    // Off by default: turning this on lets the renewal job charge saved cards.
    private boolean autoRenewEnabled = false;

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank()
                && publishableKey != null && !publishableKey.isBlank()
                && merchantId != null && !merchantId.isBlank();
    }
}
