package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.SubscriptionInvoice;
import com.legalpro.accountservice.enums.SubscriptionInvoiceStatus;
import com.legalpro.accountservice.enums.SubscriptionInvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, Long> {

    // Real cash collected in a date range -- excludes the refunded trial
    // verification charge, which was never actually revenue.
    @Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0)
        FROM SubscriptionInvoice i
        WHERE i.status = 'PAID'
        AND i.invoiceType <> 'VERIFICATION'
        AND i.paidAt >= :start AND i.paidAt < :end
        AND i.deletedAt IS NULL
    """)
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // admin billing overview "recent declines"
    List<SubscriptionInvoice> findTop10ByStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(SubscriptionInvoiceStatus status);

    // admin per-lawyer billing detail
    List<SubscriptionInvoice> findByUserUuidAndDeletedAtIsNullOrderByPeriodStartDesc(UUID userUuid);

    Optional<SubscriptionInvoice> findByUuid(UUID uuid);

    // open first-payment invoices for a payer (reused on retry, voided when the plan changes)
    List<SubscriptionInvoice> findByUserUuidAndInvoiceTypeAndStatusAndDeletedAtIsNull(
            UUID userUuid, SubscriptionInvoiceType invoiceType, SubscriptionInvoiceStatus status);

    // the renewal invoice for a given period (created on the first attempt, reused on retries)
    Optional<SubscriptionInvoice> findFirstByUserSubscriptionUuidAndInvoiceTypeAndPeriodStartAndDeletedAtIsNull(
            UUID userSubscriptionUuid, SubscriptionInvoiceType invoiceType, LocalDate periodStart);

    // billing history: newest period first
    List<SubscriptionInvoice> findByUserUuidAndDeletedAtIsNullOrderByPeriodStartDescIdDesc(UUID userUuid);

    // cancel: open renewal invoices of a subscription (DUE / OVERDUE) to void
    List<SubscriptionInvoice> findByUserSubscriptionUuidAndInvoiceTypeAndStatusInAndDeletedAtIsNull(
            UUID userSubscriptionUuid, SubscriptionInvoiceType invoiceType, Collection<SubscriptionInvoiceStatus> statuses);

    // the latest paid INITIAL / RENEWAL invoice: the billing period a renewal or a seat
    // addition is based on (excludes SEAT_ADDITION invoices, which start mid-cycle)
    Optional<SubscriptionInvoice> findFirstByUserSubscriptionUuidAndStatusAndInvoiceTypeInAndDeletedAtIsNullOrderByPeriodStartDesc(
            UUID userSubscriptionUuid, SubscriptionInvoiceStatus status, Collection<SubscriptionInvoiceType> invoiceTypes);
}
