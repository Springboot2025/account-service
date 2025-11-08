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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeAccountServiceImpl implements StripeAccountService {

    private final StripeAccountRepository stripeAccountRepository;
    private final StripeAccountMapper stripeAccountMapper;

    @PostConstruct
    public void init() {
        String secretKey = System.getenv("STRIPE_SECRET_KEY");

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("❌ Missing STRIPE_SECRET_KEY environment variable");
        }

        Stripe.apiKey = secretKey;

        log.info("✅ Stripe initialized (key prefix: {})", secretKey.substring(0, 8));
    }

    @Override
    public String createOrGetOnboardingLink(UUID lawyerUuid, String returnUrl, String refreshUrl) {
        StripeAccount sa = stripeAccountRepository.findByLawyerUuid(lawyerUuid).orElse(null);

        String accountId;

        try {
            if (sa == null) {

                log.info("✅ Creating new AU Stripe Express account with capabilities for lawyer {}", lawyerUuid);

                Account account = Account.create(
                        AccountCreateParams.builder()
                                .setType(AccountCreateParams.Type.EXPRESS)
                                .setCountry("AU")
                                .setCapabilities(
                                        AccountCreateParams.Capabilities.builder()
                                                .setCardPayments(
                                                        AccountCreateParams.Capabilities.CardPayments.builder()
                                                                .setRequested(true)
                                                                .build()
                                                )
                                                .setTransfers(
                                                        AccountCreateParams.Capabilities.Transfers.builder()
                                                                .setRequested(true)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );

                accountId = account.getId();

                sa = stripeAccountRepository.save(
                        StripeAccount.builder()
                                .lawyerUuid(lawyerUuid)
                                .stripeAccountId(accountId)
                                .chargesEnabled(false)
                                .payoutsEnabled(false)
                                .build()
                );

                log.info("✅ Stripe Connect Account created: {}", accountId);

            } else {
                accountId = sa.getStripeAccountId();
                log.info("ℹ️ Reusing existing Stripe Connect Account {}", accountId);
            }

            // Generate onboarding link
            AccountLink link = AccountLink.create(
                    AccountLinkCreateParams.builder()
                            .setAccount(accountId)
                            .setRefreshUrl(refreshUrl)
                            .setReturnUrl(returnUrl)
                            .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                            .build()
            );

            log.info("🔗 Onboarding link created for lawyer {}: {}", lawyerUuid, link.getUrl());
            return link.getUrl();

        } catch (Exception e) {
            log.error("❌ Failed to create onboarding link for lawyer {}", lawyerUuid, e);
            throw new RuntimeException("Stripe onboarding link creation failed", e);
        }
    }

    @Override
    public StripeAccountDto getStripeAccountStatus(UUID lawyerUuid) {
        StripeAccount sa = stripeAccountRepository.findByLawyerUuid(lawyerUuid).orElse(null);
        if (sa == null) return null;

        try {
            Account account = Account.retrieve(sa.getStripeAccountId());

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
