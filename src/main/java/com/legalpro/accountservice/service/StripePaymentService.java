package com.legalpro.accountservice.service;

import java.util.UUID;

public interface StripePaymentService {
    String createTrustDepositCheckout(UUID lawyerUuid, Long amountCents);
}
