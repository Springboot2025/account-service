package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.payway.BillingHistoryItemDto;
import com.legalpro.accountservice.dto.payway.CancelSubscriptionRequest;
import com.legalpro.accountservice.dto.payway.CancelSubscriptionResponse;
import com.legalpro.accountservice.dto.payway.MakePaymentRequest;
import com.legalpro.accountservice.dto.payway.PayWayPaymentResponse;
import com.legalpro.accountservice.dto.payway.PayWayTransactionResponse;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Company;
import com.legalpro.accountservice.entity.PayWayTransaction;
import com.legalpro.accountservice.entity.Subscription;
import com.legalpro.accountservice.entity.SubscriptionInvoice;
import com.legalpro.accountservice.entity.UserSubscription;
import com.legalpro.accountservice.enums.PayWayTransactionStatus;
import com.legalpro.accountservice.enums.SubscriptionInvoiceStatus;
import com.legalpro.accountservice.enums.SubscriptionInvoiceType;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.CompanyRepository;
import com.legalpro.accountservice.repository.PayWayTransactionRepository;
import com.legalpro.accountservice.repository.SubscriptionInvoiceRepository;
import com.legalpro.accountservice.repository.SubscriptionRepository;
import com.legalpro.accountservice.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Make a payment:
 *   1. validate the plan and amounts
 *   2. create (or reuse) the DUE invoice for the first billing period, and save a
 *      PENDING payway_transactions row against it (DB generates both numbers)
 *   3. PayWay PUT /customers/{customerNumber}: create the customer, save the card (uses the token)
 *   4. PayWay POST /transactions: charge the saved card by customerNumber
 *   5. save PayWay's response on the transaction; if approved, mark the invoice PAID
 *      and activate the subscription in user_subscriptions
 * Payers with an active subscription can't pay again (409), and only one payment
 * per payer can be in flight at a time (409).
 *
 * Renewals (renewDueSubscriptions, run by SubscriptionRenewalJob): when renews_at
 * comes round, create a RENEWAL invoice and charge the saved card. Declined ->
 * invoice OVERDUE, retried once a day; after 3 declines the subscription goes inactive.
 * The PayWay call is made outside any DB transaction so a slow gateway never
 * holds a connection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayWayPaymentService {

    public static final String USER_TYPE_INDIVIDUAL = "INDIVIDUAL";
    public static final String USER_TYPE_FIRM = "FIRM";

    private static final DateTimeFormatter SETTLEMENT_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final ZoneId BILLING_ZONE = ZoneId.of("Australia/Melbourne");
    private static final int MAX_RENEWAL_ATTEMPTS = 3;

    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PayWayTransactionRepository transactionRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PayWayClient payWayClient;
    private final TransactionTemplate transactionTemplate;

    public PayWayPaymentResponse makePayment(UUID userUuid, MakePaymentRequest request) {
        String duration = request.getPlanDuration().toLowerCase();
        Account account = loadPayer(userUuid);

        // 1. Validate against the plan in the DB
        Subscription plan = subscriptionRepository.findById(request.getPlanId())
                .filter(p -> p.getRemovedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        BigDecimal planPrice = "yearly".equals(duration) ? plan.getAnnualPrice() : plan.getMonthlyPrice();
        if (request.getAmount().compareTo(planPrice) != 0) {
            throw new IllegalArgumentException(
                    "amount " + request.getAmount() + " does not match the " + duration + " plan price " + planPrice);
        }
        if (request.getAmount().add(request.getGstAmount()).compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("totalAmount must equal amount + gstAmount");
        }

        String userType = account.isCompany() ? USER_TYPE_FIRM : USER_TYPE_INDIVIDUAL;
        UUID companyUuid = account.isCompany() ? account.getCompanyUuid() : null;
        String customerNumber = customerNumberFor(account);

        // 2. Invoice for the first period + the payment attempt, saved before calling PayWay
        PayWayTransaction txn;
        try {
            txn = transactionTemplate.execute(status -> {
                userSubscriptionRepository.findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(userUuid)
                        .filter(sub -> sub.getStatus() == UserSubscription.STATUS_ACTIVE)
                        .ifPresent(sub -> {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                    "You already have an active subscription");
                        });

                SubscriptionInvoice invoice = openInitialInvoice(userUuid, userType, companyUuid, plan, duration, request);
                return transactionRepository.saveAndFlush(PayWayTransaction.builder()
                        .invoiceUuid(invoice.getUuid())
                        .userUuid(userUuid)
                        .userType(userType)
                        .companyUuid(companyUuid)
                        .planId(plan.getId())
                        .planDuration(duration)
                        .customerNumber(customerNumber)
                        .amount(request.getAmount())
                        .gstAmount(request.getGstAmount())
                        .totalAmount(request.getTotalAmount())
                        .build());
            });
        } catch (DataIntegrityViolationException e) {
            // uq_payway_transactions_one_pending: another payment by this payer is in flight
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A payment is already in progress, please wait a moment");
        }
        log.info("💳 Payment {} started by {} ({} {} {})", txn.getOrderNumber(), userUuid,
                plan.getPlanName(), duration, txn.getTotalAmount());

        // 3. Create the PayWay customer with the card, then 4. charge it
        PayWayTransactionResponse result;
        try {
            payWayClient.saveCustomer(txn, request.getSingleUseTokenId(), payerName(account), account.getEmail());
            result = payWayClient.createTransaction(txn);
        } catch (RuntimeException e) {
            // Never leave the row PENDING: it would block this payer's next attempt
            String reason = e instanceof ResponseStatusException rse ? rse.getReason() : "Unexpected error";
            txn.setStatus(PayWayTransactionStatus.ERROR);
            txn.setErrorMessage(reason);
            transactionRepository.save(txn);
            log.warn("❌ Payment {} failed: {}", txn.getOrderNumber(), reason, e);
            throw e;
        }

        // 5. Save PayWay's response; an approved payment pays the invoice and activates the subscription
        applyResult(txn, result);
        PayWayTransaction savedTxn = txn;
        Outcome outcome = transactionTemplate.execute(status -> {
            transactionRepository.save(savedTxn);
            SubscriptionInvoice inv = invoiceRepository.findByUuid(savedTxn.getInvoiceUuid()).orElseThrow();
            UserSubscription sub = null;
            if (savedTxn.getStatus() == PayWayTransactionStatus.APPROVED) {
                sub = activateSubscription(savedTxn, inv);
                inv.setStatus(SubscriptionInvoiceStatus.PAID);
                inv.setPaidAt(LocalDateTime.now());
                inv.setUserSubscriptionUuid(sub.getUuid());
                inv = invoiceRepository.save(inv);
            }
            return new Outcome(inv, sub);
        });
        log.info("💳 Payment {} {}: {} {} (invoice {} {})", txn.getOrderNumber(), txn.getStatus(),
                result.getResponseCode(), result.getResponseText(),
                outcome.invoice().getInvoiceNumber(), outcome.invoice().getStatus());

        return toResponse(txn, outcome.invoice(), outcome.subscription());
    }

    private record Outcome(SubscriptionInvoice invoice, UserSubscription subscription) {
    }

    // ---------------------------------------------------------------------
    // Cancel
    // ---------------------------------------------------------------------

    /**
     * Stops renewals: status -> cancelled, so the renewal job no longer charges the
     * saved card. Access continues until renews_at; no refund. Open renewal invoices
     * (DUE / OVERDUE) are voided so they stop being retried.
     *
     * Then, as a second lock, PayWay "stop all payments" on the customer, so nothing
     * can charge the card even by mistake. The DB cancel is what counts: if the PayWay
     * call fails the cancel still succeeds (paywayPaymentsStopped = false, logged and
     * audited). Saving a card on re-subscribe clears PayWay's stop flag.
     */
    public CancelSubscriptionResponse cancelSubscription(UUID userUuid, CancelSubscriptionRequest request) {
        loadPayer(userUuid);
        String reason = request != null && request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim() : null;

        CancelSubscriptionResponse response = transactionTemplate.execute(status -> {
            UserSubscription sub = userSubscriptionRepository.findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(userUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription found"));
            if (sub.getStatus() != UserSubscription.STATUS_ACTIVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No active subscription to cancel");
            }

            List<SubscriptionInvoice> openRenewals = invoiceRepository
                    .findByUserSubscriptionUuidAndInvoiceTypeAndStatusInAndDeletedAtIsNull(sub.getUuid(),
                            SubscriptionInvoiceType.RENEWAL,
                            List.of(SubscriptionInvoiceStatus.DUE, SubscriptionInvoiceStatus.OVERDUE));
            openRenewals.forEach(inv -> inv.setStatus(SubscriptionInvoiceStatus.VOID));
            invoiceRepository.saveAll(openRenewals);

            LocalDateTime now = LocalDateTime.now();
            sub.setStatus(UserSubscription.STATUS_CANCELLED);
            sub.setCancelledAt(now);
            sub.setCancelReason(reason);
            sub.setUpdatedAt(now);
            sub = userSubscriptionRepository.save(sub);
            log.info("🛑 Subscription {} cancelled by {}, access until {}, {} open renewal invoice(s) voided",
                    sub.getUuid(), userUuid, sub.getRenewsAt(), openRenewals.size());

            String planName = subscriptionRepository.findById(sub.getPlanId())
                    .map(Subscription::getPlanName).orElse(null);
            return CancelSubscriptionResponse.builder()
                    .subscriptionUuid(sub.getUuid())
                    .status(sub.getStatus())
                    .planId(sub.getPlanId())
                    .planName(planName)
                    .planDuration(sub.getPlanDuration())
                    .startDate(sub.getStartDate())
                    .accessUntil(sub.getRenewsAt())
                    .cancelledAt(sub.getCancelledAt())
                    .cancelReason(sub.getCancelReason())
                    .voidedInvoices(openRenewals.size())
                    .paywayPaymentsStopped(false)
                    .build();
        });

        // Second lock on PayWay, outside the DB transaction
        userSubscriptionRepository.findByUuid(response.getSubscriptionUuid())
                .map(UserSubscription::getPaywayCustomerNumber)
                .ifPresent(customerNumber -> {
                    try {
                        payWayClient.stopPayments(userUuid, customerNumber);
                        response.setPaywayPaymentsStopped(true);
                        log.info("🔒 PayWay payments stopped for {}", customerNumber);
                    } catch (RuntimeException e) {
                        log.warn("⚠️ Subscription {} cancelled, but stopping PayWay payments for {} failed: {}",
                                response.getSubscriptionUuid(), customerNumber, e.getMessage());
                    }
                });
        return response;
    }

    // ---------------------------------------------------------------------
    // Billing history
    // ---------------------------------------------------------------------

    /**
     * The payer's invoices, newest first: PAID, OVERDUE, and renewals still DUE.
     * Hidden: VOID invoices, and unpaid first-payment invoices left by a declined
     * or abandoned checkout (never a real bill).
     */
    public List<BillingHistoryItemDto> getBillingHistory(UUID userUuid) {
        loadPayer(userUuid);

        List<SubscriptionInvoice> invoices = invoiceRepository
                .findByUserUuidAndDeletedAtIsNullOrderByPeriodStartDescIdDesc(userUuid).stream()
                .filter(PayWayPaymentService::isBillingHistoryInvoice)
                .toList();
        if (invoices.isEmpty()) {
            return List.of();
        }

        Map<Long, Subscription> plans = subscriptionRepository.findAll().stream()
                .collect(Collectors.toMap(Subscription::getId, Function.identity()));
        Map<UUID, PayWayTransaction> approvedPayments = transactionRepository
                .findByInvoiceUuidInAndStatus(invoices.stream().map(SubscriptionInvoice::getUuid).toList(),
                        PayWayTransactionStatus.APPROVED).stream()
                .collect(Collectors.toMap(PayWayTransaction::getInvoiceUuid, Function.identity(), (a, b) -> a));

        return invoices.stream()
                .map(inv -> toBillingHistoryItem(inv, plans.get(inv.getPlanId()), approvedPayments.get(inv.getUuid())))
                .toList();
    }

    private static boolean isBillingHistoryInvoice(SubscriptionInvoice invoice) {
        return switch (invoice.getStatus()) {
            case PAID, OVERDUE -> true;
            case DUE -> invoice.getInvoiceType() == SubscriptionInvoiceType.RENEWAL;
            case VOID -> false;
        };
    }

    private static BillingHistoryItemDto toBillingHistoryItem(SubscriptionInvoice inv, Subscription plan,
                                                              PayWayTransaction payment) {
        String planName = plan != null ? plan.getPlanName() : null;
        String durationLabel = "yearly".equals(inv.getPlanDuration()) ? "Yearly" : "Monthly";
        return BillingHistoryItemDto.builder()
                .invoiceUuid(inv.getUuid())
                .invoiceNumber(inv.getInvoiceNumber())
                .invoiceType(inv.getInvoiceType().name())
                .date(inv.getPeriodStart())
                .description(planName != null ? planName + " — " + durationLabel : durationLabel)
                .planId(inv.getPlanId())
                .planName(planName)
                .planDuration(inv.getPlanDuration())
                .periodStart(inv.getPeriodStart())
                .periodEnd(inv.getPeriodEnd())
                .amount(inv.getAmount())
                .gstAmount(inv.getGstAmount())
                .totalAmount(inv.getTotalAmount())
                .currency(inv.getCurrency())
                .status(inv.getStatus().name())
                .paidAt(inv.getPaidAt())
                .orderNumber(payment != null ? payment.getOrderNumber() : null)
                .receiptNumber(payment != null ? payment.getReceiptNumber() : null)
                .cardScheme(payment != null ? payment.getCardScheme() : null)
                .maskedCardNumber(payment != null ? payment.getMaskedCardNumber() : null)
                .build();
    }

    // ---------------------------------------------------------------------
    // Renewals
    // ---------------------------------------------------------------------

    public void renewDueSubscriptions() {
        // renews_at holds Melbourne dates, so compare with Melbourne time (servers run on UTC)
        List<Long> dueIds = userSubscriptionRepository.findIdsDueForRenewal(LocalDateTime.now(BILLING_ZONE));
        log.info("🔁 {} subscription(s) due for renewal", dueIds.size());

        for (Long id : dueIds) {
            try {
                renewSubscription(id);
            } catch (Exception e) {
                // One bad row must not stop the rest
                log.error("❌ Renewal failed for user_subscription {}", id, e);
            }
        }
    }

    private void renewSubscription(Long subscriptionId) {
        // 1. Re-check the subscription (another instance may have renewed it), get the
        //    invoice for the new period, and save a PENDING attempt
        PayWayTransaction txn;
        try {
            txn = transactionTemplate.execute(status -> startRenewalAttempt(subscriptionId));
        } catch (DataIntegrityViolationException e) {
            log.info("⏭️ Subscription {} already has a payment in progress, skipping", subscriptionId);
            return;
        }
        if (txn == null) {
            return;
        }

        // 2. Charge the saved card
        PayWayTransactionResponse result = null;
        boolean cardRejected = false;
        try {
            result = payWayClient.createTransaction(txn);
            applyResult(txn, result);
        } catch (RuntimeException e) {
            txn.setStatus(PayWayTransactionStatus.ERROR);
            txn.setErrorMessage(e instanceof ResponseStatusException rse ? rse.getReason() : "Unexpected error");
            // 422 = PayWay rejected the card (e.g. expired): the customer's problem, counts as a failed attempt
            cardRejected = e instanceof ResponseStatusException rse && rse.getStatusCode().value() == 422;
        }
        // Declined or card rejected counts; PayWay down / still pending doesn't
        boolean failedAttempt = txn.getStatus() == PayWayTransactionStatus.DECLINED || cardRejected;

        // 3. Record the result and move the subscription on
        PayWayTransaction savedTxn = txn;
        transactionTemplate.executeWithoutResult(status -> {
            transactionRepository.save(savedTxn);
            SubscriptionInvoice invoice = invoiceRepository.findByUuid(savedTxn.getInvoiceUuid()).orElseThrow();
            UserSubscription sub = userSubscriptionRepository.findById(subscriptionId).orElseThrow();
            LocalDateTime now = LocalDateTime.now();

            if (savedTxn.getStatus() == PayWayTransactionStatus.APPROVED) {
                invoice.setStatus(SubscriptionInvoiceStatus.PAID);
                invoice.setPaidAt(now);
                sub.setRenewsAt(invoice.getPeriodEnd().atStartOfDay());
                log.info("✅ Subscription {} renewed ({}), next billing {}", sub.getUuid(),
                        invoice.getInvoiceNumber(), sub.getRenewsAt());
            } else if (failedAttempt) {
                invoice.setStatus(SubscriptionInvoiceStatus.OVERDUE);
                invoice.setFailedAttempts(invoice.getFailedAttempts() + 1);
                if (invoice.getFailedAttempts() >= MAX_RENEWAL_ATTEMPTS) {
                    sub.setStatus(UserSubscription.STATUS_INACTIVE);
                    log.warn("⛔ Subscription {} deactivated: {} declined {} times", sub.getUuid(),
                            invoice.getInvoiceNumber(), invoice.getFailedAttempts());
                } else {
                    log.warn("⚠️ Renewal {} declined (attempt {}/{}), retrying tomorrow", invoice.getInvoiceNumber(),
                            invoice.getFailedAttempts(), MAX_RENEWAL_ATTEMPTS);
                }
            } else {
                log.error("❌ Renewal {} not completed ({}), retrying tomorrow without counting it",
                        invoice.getInvoiceNumber(), savedTxn.getErrorMessage());
            }
            sub.setUpdatedAt(now);
            invoiceRepository.save(invoice);
            userSubscriptionRepository.save(sub);
        });
    }

    /** Returns the PENDING attempt to charge, or null when there is nothing to do. */
    private PayWayTransaction startRenewalAttempt(Long subscriptionId) {
        UserSubscription sub = userSubscriptionRepository.findById(subscriptionId).orElse(null);
        if (sub == null || sub.getDeletedAt() != null || sub.getStatus() != UserSubscription.STATUS_ACTIVE
                || sub.getPaywayCustomerNumber() == null || sub.getRenewsAt() == null
                || sub.getRenewsAt().isAfter(LocalDateTime.now(BILLING_ZONE))) {
            return null;
        }

        // Renewals charge what the subscriber last paid
        SubscriptionInvoice lastPaid = invoiceRepository
                .findFirstByUserSubscriptionUuidAndStatusAndDeletedAtIsNullOrderByPeriodStartDesc(
                        sub.getUuid(), SubscriptionInvoiceStatus.PAID)
                .orElse(null);
        if (lastPaid == null) {
            log.warn("⚠️ Subscription {} has no paid invoice to renew from, skipping", sub.getUuid());
            return null;
        }

        // The new period starts on the billing date, even if the charge succeeds a few days late
        LocalDate periodStart = sub.getRenewsAt().toLocalDate();
        SubscriptionInvoice invoice = invoiceRepository
                .findFirstByUserSubscriptionUuidAndInvoiceTypeAndPeriodStartAndDeletedAtIsNull(
                        sub.getUuid(), SubscriptionInvoiceType.RENEWAL, periodStart)
                .orElse(null);
        if (invoice == null) {
            invoice = invoiceRepository.saveAndFlush(SubscriptionInvoice.builder()
                    .userUuid(sub.getUserUuid())
                    .userType(sub.getUserType())
                    .companyUuid(lastPaid.getCompanyUuid())
                    .userSubscriptionUuid(sub.getUuid())
                    .invoiceType(SubscriptionInvoiceType.RENEWAL)
                    .planId(sub.getPlanId())
                    .planDuration(sub.getPlanDuration())
                    .periodStart(periodStart)
                    .periodEnd(addPeriod(periodStart, sub.getPlanDuration()))
                    .dueDate(periodStart)
                    .amount(lastPaid.getAmount())
                    .gstAmount(lastPaid.getGstAmount())
                    .totalAmount(lastPaid.getTotalAmount())
                    .currency(lastPaid.getCurrency())
                    .build());
        } else if (invoice.getStatus() == SubscriptionInvoiceStatus.PAID) {
            // This period is already paid but renews_at wasn't moved on: fix it instead of
            // picking the subscription up as due every night
            sub.setRenewsAt(invoice.getPeriodEnd().atStartOfDay());
            userSubscriptionRepository.save(sub);
            log.warn("⚠️ Subscription {} period {} already paid, renews_at corrected to {}",
                    sub.getUuid(), periodStart, sub.getRenewsAt());
            return null;
        } else if (invoice.getStatus() == SubscriptionInvoiceStatus.VOID) {
            return null;
        }

        // At most one attempt per invoice per day, even with several app instances
        if (transactionRepository.existsByInvoiceUuidAndCreatedAtAfter(
                invoice.getUuid(), LocalDateTime.now().minusHours(20))) {
            return null;
        }

        return transactionRepository.saveAndFlush(PayWayTransaction.builder()
                .invoiceUuid(invoice.getUuid())
                .userUuid(invoice.getUserUuid())
                .userType(invoice.getUserType())
                .companyUuid(invoice.getCompanyUuid())
                .planId(invoice.getPlanId())
                .planDuration(invoice.getPlanDuration())
                .customerNumber(sub.getPaywayCustomerNumber())
                .amount(invoice.getAmount())
                .gstAmount(invoice.getGstAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .build());
    }

    private static LocalDate addPeriod(LocalDate from, String duration) {
        return "yearly".equals(duration) ? from.plusYears(1) : from.plusMonths(1);
    }

    /**
     * Turns the payer's subscription on for the paid period: updates their
     * existing user_subscriptions row (e.g. inactive or cancelled) or creates one.
     * renews_at = the invoice's period_end, when the renewal job will charge again.
     */
    private UserSubscription activateSubscription(PayWayTransaction txn, SubscriptionInvoice invoice) {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = userSubscriptionRepository
                .findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(txn.getUserUuid())
                .orElseGet(() -> UserSubscription.builder()
                        .uuid(UUID.randomUUID())
                        .userUuid(txn.getUserUuid())
                        .createdAt(now)
                        .build());

        sub.setUserType(txn.getUserType());
        sub.setPlanId(txn.getPlanId());
        sub.setPlanDuration(txn.getPlanDuration());
        sub.setStatus(UserSubscription.STATUS_ACTIVE);
        sub.setCancelledAt(null);
        sub.setCancelReason(null);
        if (sub.getStartDate() == null) {
            sub.setStartDate(now); // member since
        }
        sub.setRenewsAt(invoice.getPeriodEnd().atStartOfDay());
        sub.setPaywayCustomerNumber(txn.getCustomerNumber());
        sub.setUpdatedAt(now);

        sub = userSubscriptionRepository.save(sub);
        log.info("✅ Subscription {} active for {} until {}", sub.getUuid(), sub.getUserUuid(), sub.getRenewsAt());
        return sub;
    }

    /**
     * The DUE first-payment invoice for this plan: reused when the user retries
     * (e.g. after a decline) so billing history doesn't fill up with duplicates,
     * with its period moved to start today. Open first-payment invoices for a
     * different plan / amount are voided.
     */
    private SubscriptionInvoice openInitialInvoice(UUID userUuid, String userType, UUID companyUuid,
                                                   Subscription plan, String duration, MakePaymentRequest request) {
        LocalDate today = LocalDate.now(BILLING_ZONE);
        SubscriptionInvoice reuse = null;

        for (SubscriptionInvoice open : invoiceRepository.findByUserUuidAndInvoiceTypeAndStatusAndDeletedAtIsNull(
                userUuid, SubscriptionInvoiceType.INITIAL, SubscriptionInvoiceStatus.DUE)) {
            boolean samePlan = open.getPlanId().equals(plan.getId())
                    && open.getPlanDuration().equals(duration)
                    && open.getTotalAmount().compareTo(request.getTotalAmount()) == 0;
            if (samePlan && reuse == null) {
                reuse = open;
            } else {
                open.setStatus(SubscriptionInvoiceStatus.VOID);
                invoiceRepository.save(open);
            }
        }

        SubscriptionInvoice invoice = reuse != null ? reuse : SubscriptionInvoice.builder()
                .userUuid(userUuid)
                .userType(userType)
                .companyUuid(companyUuid)
                .invoiceType(SubscriptionInvoiceType.INITIAL)
                .planId(plan.getId())
                .planDuration(duration)
                .amount(request.getAmount())
                .gstAmount(request.getGstAmount())
                .totalAmount(request.getTotalAmount())
                .build();
        invoice.setPeriodStart(today);
        invoice.setPeriodEnd(addPeriod(today, duration));
        invoice.setDueDate(today);
        return invoiceRepository.saveAndFlush(invoice);
    }

    // ---------------------------------------------------------------------

    private void applyResult(PayWayTransaction txn, PayWayTransactionResponse result) {
        if (result.isApproved()) {
            txn.setStatus(PayWayTransactionStatus.APPROVED);
        } else if (result.isPending()) {
            // Still unsettled after polling: not a decline, it may yet go through
            txn.setStatus(PayWayTransactionStatus.ERROR);
            txn.setErrorMessage("Payment still processing at PayWay (transaction "
                    + result.getTransactionId() + "), check the PayWay portal before retrying");
        } else {
            txn.setStatus(PayWayTransactionStatus.DECLINED);
        }
        txn.setPaywayTransactionId(result.getTransactionId());
        txn.setReceiptNumber(result.getReceiptNumber());
        txn.setPaywayStatus(result.getStatus());
        txn.setResponseCode(result.getResponseCode());
        txn.setResponseText(truncate(result.getResponseText(), 255));
        txn.setSurchargeAmount(result.getSurchargeAmount());
        txn.setPaymentAmount(result.getPaymentAmount());
        txn.setTransactionDateTime(truncate(result.getTransactionDateTime(), 40));
        txn.setSettlementDate(parseSettlementDate(result.getSettlementDate()));
        if (result.getCreditCard() != null) {
            txn.setCardScheme(result.getCreditCard().getCardScheme());
            txn.setMaskedCardNumber(result.getCreditCard().getCardNumber());
        }
    }

    private Account loadPayer(UUID userUuid) {
        Account account = accountRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Lawyers who joined a firm are covered by the firm admin's subscription
        if (account.getCompanyUuid() != null && !account.isCompany()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your firm's subscription is managed by the firm admin");
        }
        return account;
    }

    // LAW-<accounts.id> | FIRM-<companies.id>: stable, no personal data, within PayWay's 20 chars
    private String customerNumberFor(Account account) {
        if (account.isCompany()) {
            Company company = companyRepository.findByUuid(account.getCompanyUuid())
                    .orElseThrow(() -> new IllegalStateException("Firm account has no company"));
            return "FIRM-" + company.getId();
        }
        return "LAW-" + account.getId();
    }

    // Shown on the customer in the PayWay portal: firm name, or the lawyer's name
    private String payerName(Account account) {
        if (account.isCompany() && account.getCompanyUuid() != null) {
            String firmName = companyRepository.findByUuid(account.getCompanyUuid())
                    .map(Company::getName).orElse(null);
            if (firmName != null && !firmName.isBlank()) {
                return firmName;
            }
        }
        JsonNode pd = account.getPersonalDetails();
        if (pd != null) {
            String first = pd.hasNonNull("firstName") ? pd.get("firstName").asText() : "";
            String last = pd.hasNonNull("lastName") ? pd.get("lastName").asText() : "";
            String fullName = (first + " " + last).trim();
            if (!fullName.isEmpty()) {
                return fullName;
            }
        }
        return account.getEmail();
    }

    private static LocalDate parseSettlementDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), SETTLEMENT_DATE);
        } catch (DateTimeParseException e) {
            log.warn("⚠️ Unexpected PayWay settlementDate '{}'", value);
            return null;
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private static PayWayPaymentResponse toResponse(PayWayTransaction txn, SubscriptionInvoice invoice,
                                                    UserSubscription subscription) {
        return PayWayPaymentResponse.builder()
                .subscriptionUuid(subscription != null ? subscription.getUuid() : null)
                .subscriptionStatus(subscription != null ? subscription.getStatus() : null)
                .renewsAt(subscription != null ? subscription.getRenewsAt() : null)
                .uuid(txn.getUuid())
                .orderNumber(txn.getOrderNumber())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceStatus(invoice.getStatus().name())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .status(txn.getStatus().name())
                .planId(txn.getPlanId())
                .planDuration(txn.getPlanDuration())
                .customerNumber(txn.getCustomerNumber())
                .amount(txn.getAmount())
                .gstAmount(txn.getGstAmount())
                .totalAmount(txn.getTotalAmount())
                .currency(txn.getCurrency())
                .paywayTransactionId(txn.getPaywayTransactionId())
                .receiptNumber(txn.getReceiptNumber())
                .responseCode(txn.getResponseCode())
                .responseText(txn.getResponseText())
                .cardScheme(txn.getCardScheme())
                .maskedCardNumber(txn.getMaskedCardNumber())
                .settlementDate(txn.getSettlementDate())
                .errorMessage(txn.getErrorMessage())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
