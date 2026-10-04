package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.SubscriptionInvoice;
import com.legalpro.accountservice.enums.SubscriptionInvoiceStatus;
import com.legalpro.accountservice.enums.SubscriptionInvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, Long> {

    Optional<SubscriptionInvoice> findByUuid(UUID uuid);

    // open first-payment invoices for a payer (reused on retry, voided when the plan changes)
    List<SubscriptionInvoice> findByUserUuidAndInvoiceTypeAndStatusAndDeletedAtIsNull(
            UUID userUuid, SubscriptionInvoiceType invoiceType, SubscriptionInvoiceStatus status);

    // the renewal invoice for a given period (created on the first attempt, reused on retries)
    Optional<SubscriptionInvoice> findFirstByUserSubscriptionUuidAndInvoiceTypeAndPeriodStartAndDeletedAtIsNull(
            UUID userSubscriptionUuid, SubscriptionInvoiceType invoiceType, LocalDate periodStart);

    // the latest paid invoice: renewals charge the same amounts
    Optional<SubscriptionInvoice> findFirstByUserSubscriptionUuidAndStatusAndDeletedAtIsNullOrderByPeriodStartDesc(
            UUID userSubscriptionUuid, SubscriptionInvoiceStatus status);

    // billing history: newest period first
    List<SubscriptionInvoice> findByUserUuidAndDeletedAtIsNullOrderByPeriodStartDescIdDesc(UUID userUuid);

    // cancel: open renewal invoices of a subscription (DUE / OVERDUE) to void
    List<SubscriptionInvoice> findByUserSubscriptionUuidAndInvoiceTypeAndStatusInAndDeletedAtIsNull(
            UUID userSubscriptionUuid, SubscriptionInvoiceType invoiceType, Collection<SubscriptionInvoiceStatus> statuses);
}
