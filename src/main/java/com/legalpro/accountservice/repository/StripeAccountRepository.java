package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.StripeAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
public interface StripeAccountRepository extends JpaRepository<StripeAccount, Long> {
    Optional<StripeAccount> findByLawyerUuid(UUID lawyerUuid);
    Optional<StripeAccount> findByStripeAccountId(String stripeAccountId);
}

