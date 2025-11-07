package com.legalpro.accountservice.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface StripeCheckoutService {
    String createCheckoutSession(UUID invoiceUuid, UUID clientUuid);

}
