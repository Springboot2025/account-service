package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.StripeAccountDto;
import com.legalpro.accountservice.entity.StripeAccount;
import com.legalpro.accountservice.mapper.StripeAccountMapper;
import com.legalpro.accountservice.repository.StripeAccountRepository;
import com.legalpro.accountservice.service.StripeAccountService;
import com.stripe.Stripe;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeAccountServiceImpl implements StripeAccountService {

    private final StripeAccountRepository stripeAccountRepository;
    private final StripeAccountMapper stripeAccountMapper;

    @Value("${STRIPE_SECRET_KEY}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    public String createOrGetOnboardingLink(UUID lawyerUuid, String returnUrl, String refreshUrl) {

        // Find existing Stripe account for this lawyer
        StripeAccount sa = stripeAccountRepository.findByLawyerUuid(lawyerUuid)
                .orElse(null);

        String accountId;

        try {
            if (sa == null) {
                // Create new Stripe Express Account
                Account account = Account.create(AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .build());

                accountId = account.getId();

                // Save in our DB
                sa = stripeAccountRepository.save(
                        StripeAccount.builder()
                                .lawyerUuid(lawyerUuid)
                                .stripeAccountId(accountId)
                                .build()
                );

                log.info("✅ Created new Stripe connect account {} for Lawyer {}", accountId, lawyerUuid);

            } else {
                accountId = sa.getStripeAccountId();
            }

            // Create Stripe onboarding link
            AccountLink link = AccountLink.create(AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .setReturnUrl(returnUrl)
                    .setRefreshUrl(refreshUrl)
                    .build());

            return link.getUrl();

        } catch (Exception e) {
            log.error("❌ Failed to create onboarding link for lawyer {}", lawyerUuid, e);
            throw new RuntimeException("Stripe onboarding link creation failed", e);
        }
    }

    @Override
    public StripeAccountDto getStripeAccountStatus(UUID lawyerUuid) {
        StripeAccount sa = stripeAccountRepository.findByLawyerUuid(lawyerUuid)
                .orElse(null);

        if (sa == null) {
            return null; // no Stripe connection yet
        }

        try {
            Account account = Account.retrieve(sa.getStripeAccountId());

            // Keep flags in sync (optional but recommended)
            sa.setChargesEnabled(Boolean.TRUE.equals(account.getChargesEnabled()));
            sa.setPayoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()));
            stripeAccountRepository.save(sa);

            return stripeAccountMapper.toDto(sa);

        } catch (Exception e) {
            log.error("❌ Failed to fetch Stripe account status for lawyer {}", lawyerUuid, e);
            throw new RuntimeException("Stripe account status fetch failed", e);
        }
    }
}
