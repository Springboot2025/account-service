package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.StripeAccountDto;

import java.util.UUID;

public interface StripeAccountService {

    /**
     * Creates (or reuses) a lawyer Stripe account and returns an onboarding URL.
     */
    String createOrGetOnboardingLink(UUID lawyerUuid, String returnUrl, String refreshUrl);

    /**
     * Retrieves the current Stripe account status (connected, enabled flags).
     */
    StripeAccountDto getStripeAccountStatus(UUID lawyerUuid);
}
