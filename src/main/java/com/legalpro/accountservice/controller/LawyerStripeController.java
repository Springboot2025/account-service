package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.StripeAccountDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.StripeAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lawyer/stripe")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerStripeController {

    private final StripeAccountService stripeAccountService;

    @PostMapping("/onboard")
    public ResponseEntity<ApiResponse<Map<String,String>>> onboard(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String url = stripeAccountService.createOrGetOnboardingLink(
                user.getUuid(),
                "https://lawproject-nu.vercel.app/stripe/onboarding",
                "https://lawproject-nu.vercel.app/stripe/onboarding"
        );

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Stripe onboarding link created",
                Map.of("url", url)
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<StripeAccountDto>> status(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        StripeAccountDto status = stripeAccountService.getStripeAccountStatus(user.getUuid());

        if (status == null) {
            return ResponseEntity.ok(ApiResponse.success(
                    200,
                    "No Stripe account connected",
                    null
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Stripe account status fetched",
                status
        ));
    }
}
