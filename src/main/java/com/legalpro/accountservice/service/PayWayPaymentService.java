package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.payway.BillingHistoryItemDto;
import com.legalpro.accountservice.dto.payway.CancelSubscriptionRequest;
import com.legalpro.accountservice.dto.payway.CancelSubscriptionResponse;
import com.legalpro.accountservice.dto.payway.MakePaymentRequest;
import com.legalpro.accountservice.dto.payway.PayWayCreditCard;
import com.legalpro.accountservice.dto.payway.PayWayPaymentSetup;
import com.legalpro.accountservice.dto.payway.SaveCardRequest;
import com.legalpro.accountservice.dto.payway.SavedCardResponse;
import com.legalpro.accountservice.dto.payway.PayWayPaymentResponse;
import com.legalpro.accountservice.dto.payway.PayWayTransactionResponse;
import com.legalpro.accountservice.dto.payway.SubscriptionStatusResponse;
import com.legalpro.accountservice.dto.payway.UpdateSeatsResponse;
import com.legalpro.accountservice.config.PayWayProperties;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Company;
import com.legalpro.accountservice.entity.PayWayTransaction;
import com.legalpro.accountservice.entity.Subscription;
import com.legalpro.accountservice.entity.SubscriptionInvoice;
import com.legalpro.accountservice.entity.UserSubscription;
import com.legalpro.accountservice.enums.PayWayTransactionStatus;
import com.legalpro.accountservice.enums.SubscriptionInvoiceStatus;
import com.legalpro.accountservice.enums.SubscriptionInvoiceType;
import com.legalpro.accountservice.entity.SystemSetting;
import com.legalpro.accountservice.entity.TrialReminderSend;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.CompanyRepository;
import com.legalpro.accountservice.repository.PayWayTransactionRepository;
import com.legalpro.accountservice.repository.SubscriptionInvoiceRepository;
import com.legalpro.accountservice.repository.SubscriptionRepository;
import com.legalpro.accountservice.repository.SystemSettingRepository;
import com.legalpro.accountservice.repository.TrialReminderSendRepository;
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
import java.util.Optional;
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
    private static final BigDecimal GST_RATE = new BigDecimal("0.10");

    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PayWayTransactionRepository transactionRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final TrialReminderSendRepository trialReminderSendRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PayWayClient payWayClient;
    private final PayWayProperties payWayProperties;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    // Fallback reminder schedule when system_settings has nothing configured yet
    private static final List<Integer> DEFAULT_TRIAL_REMINDER_DAYS = List.of(5, 1);

    /**
     * subscriptions.seat_price is documented as a MONTHLY per-seat rate (see
     * Subscription.seatPrice) -- same convention as the base plan's own
     * monthly_price/annual_price split. A yearly plan must therefore charge
     * 12x this rate per seat per year, exactly like its base price is ~12x
     * the monthly one; charging the flat monthly rate once for a whole year
     * would make seats on a yearly plan ~12x cheaper than on a monthly one.
     */
    private static BigDecimal seatCostFor(Subscription plan, String duration, int seatCount) {
        if (plan.getSeatPrice() == null || seatCount <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal perSeat = "yearly".equals(duration)
                ? plan.getSeatPrice().multiply(BigDecimal.valueOf(12))
                : plan.getSeatPrice();
        return perSeat.multiply(BigDecimal.valueOf(seatCount));
    }

    public PayWayPaymentResponse makePayment(UUID userUuid, MakePaymentRequest request) {
        String duration = request.getPlanDuration().toLowerCase();
        Account account = loadPayer(userUuid);

        // 1. Validate against the plan in the DB
        Subscription plan = subscriptionRepository.findById(request.getPlanId())
                .filter(p -> p.getRemovedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        int additionalSeats = request.getAdditionalSeats() != null ? request.getAdditionalSeats() : 0;
        if (additionalSeats > 0 && plan.getSeatPrice() == null) {
            throw new IllegalArgumentException("This plan does not support additional seats");
        }

        BigDecimal planPrice = "yearly".equals(duration) ? plan.getAnnualPrice() : plan.getMonthlyPrice();
        BigDecimal seatCost = seatCostFor(plan, duration, additionalSeats);
        BigDecimal expectedAmount = planPrice.add(seatCost);
        if (request.getAmount().compareTo(expectedAmount) != 0) {
            throw new IllegalArgumentException(
                    "amount " + request.getAmount() + " does not match the " + duration + " plan price " + expectedAmount);
        }
        if (request.getAmount().add(request.getGstAmount()).compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("totalAmount must equal amount + gstAmount");
        }

        String userType = account.isCompany() ? USER_TYPE_FIRM : USER_TYPE_INDIVIDUAL;
        UUID companyUuid = account.isCompany() ? account.getCompanyUuid() : null;
        String customerNumber = customerNumberFor(account);

        // A plan with trial_days > 0 gets a free trial instead of being charged
        // today -- unless this account has already used its one trial before
        // (the same user_subscriptions row is reused across cancel/resubscribe,
        // so hasUsedTrial survives that and is never reset).
        int trialDays = plan.getTrialDays() != null ? plan.getTrialDays() : 0;
        boolean usedTrialBefore = userSubscriptionRepository.findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(userUuid)
                .map(UserSubscription::getHasUsedTrial).map(Boolean.TRUE::equals).orElse(false);
        if (trialDays > 0 && !usedTrialBefore) {
            return startTrialSubscription(userUuid, account, plan, duration, request, additionalSeats,
                    userType, companyUuid, customerNumber, trialDays);
        }

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

                SubscriptionInvoice invoice = openInitialInvoice(userUuid, userType, companyUuid, plan, duration, request, additionalSeats);
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
                        .seatCount(additionalSeats)
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
    // Save card (update payment method)
    // ---------------------------------------------------------------------

    /**
     * Replaces the card saved on the payer's PayWay customer. Only for an active
     * subscription: that's what renewals charge, and an OVERDUE renewal is retried
     * with the new card on the next nightly run. Not allowed once cancelled, because
     * saving a card would undo PayWay's "stop payments" lock set on cancel.
     */
    public SavedCardResponse saveCard(UUID userUuid, SaveCardRequest request) {
        loadPayer(userUuid);

        UserSubscription sub = userSubscriptionRepository.findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(userUuid)
                .filter(s -> s.getPaywayCustomerNumber() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No saved card yet, please make a payment first"));
        if (sub.getStatus() != UserSubscription.STATUS_ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No active subscription: the card can only be changed on an active subscription");
        }

        PayWayPaymentSetup setup = payWayClient.updateCard(userUuid, sub.getPaywayCustomerNumber(),
                request.getSingleUseTokenId());

        boolean overdue = !invoiceRepository.findByUserSubscriptionUuidAndInvoiceTypeAndStatusInAndDeletedAtIsNull(
                sub.getUuid(), SubscriptionInvoiceType.RENEWAL, List.of(SubscriptionInvoiceStatus.OVERDUE)).isEmpty();
        PayWayCreditCard card = setup.getCreditCard();
        log.info("💳 Card updated for {} ({}){}", sub.getPaywayCustomerNumber(),
                card != null ? card.getCardNumber() : "?", overdue ? ", overdue renewal will retry tonight" : "");

        return SavedCardResponse.builder()
                .customerNumber(sub.getPaywayCustomerNumber())
                .cardScheme(card != null ? card.getCardScheme() : null)
                .maskedCardNumber(card != null ? card.getCardNumber() : null)
                .expiryDateMonth(card != null ? card.getExpiryDateMonth() : null)
                .expiryDateYear(card != null ? card.getExpiryDateYear() : null)
                .cardholderName(card != null ? card.getCardholderName() : null)
                .overdueRenewal(overdue)
                .build();
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
    // Subscription status
    // ---------------------------------------------------------------------

    /** The logged-in payer's current subscription, read fresh from the DB. */
    public SubscriptionStatusResponse getSubscriptionStatus(UUID userUuid) {
        Account account = loadPayer(userUuid);
        UserSubscription sub = userSubscriptionRepository
                .findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(userUuid)
                .orElse(null);
        if (sub == null) {
            return SubscriptionStatusResponse.builder().subscribed(false).build();
        }

        Subscription plan = subscriptionRepository.findById(sub.getPlanId()).orElse(null);
        boolean isFirm = account.isCompany();
        int seatCount = sub.getSeatCount() != null ? sub.getSeatCount() : 0;
        BigDecimal amount = null;
        BigDecimal gstAmount = null;
        BigDecimal totalAmount = null;
        if (plan != null) {
            BigDecimal planPrice = "yearly".equals(sub.getPlanDuration()) ? plan.getAnnualPrice() : plan.getMonthlyPrice();
            amount = planPrice.add(seatCostFor(plan, sub.getPlanDuration(), seatCount));
            gstAmount = amount.multiply(GST_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
            totalAmount = amount.add(gstAmount);
        }

        return SubscriptionStatusResponse.builder()
                .subscribed(sub.getStatus() == UserSubscription.STATUS_ACTIVE)
                .subscriptionUuid(sub.getUuid())
                .planId(sub.getPlanId())
                .planName(plan != null ? plan.getPlanName() : null)
                .planDuration(sub.getPlanDuration())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .renewsAt(sub.getRenewsAt())
                .cancelledAt(sub.getCancelledAt())
                .monthlyAmount(amount)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .isFirm(isFirm)
                .additionalSeats(isFirm ? seatCount : null)
                .usedSeats(isFirm ? accountRepository.countByCompanyUuidAndIsCompanyFalse(account.getCompanyUuid()) : null)
                .build();
    }

    // ---------------------------------------------------------------------
    // Manage seats (FIRM only)
    // ---------------------------------------------------------------------

    /**
     * Sets the firm's additional-seat count to the given new total (not a delta).
     * Adding seats charges the saved card immediately, pro-rata for the days left
     * in the current billing cycle; removing seats takes effect at the next
     * renewal (SubscriptionRenewalJob recomputes from the new, lower count).
     */
    public UpdateSeatsResponse updateSeats(UUID userUuid, int newSeatCount) {
        Account account = loadPayer(userUuid);
        if (!account.isCompany()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a firm admin can manage seats");
        }

        UserSubscription sub = userSubscriptionRepository.findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(userUuid)
                .filter(s -> s.getStatus() == UserSubscription.STATUS_ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active subscription"));

        Subscription plan = subscriptionRepository.findById(sub.getPlanId())
                .orElseThrow(() -> new IllegalStateException("Plan not found"));
        if (plan.getSeatPrice() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This plan does not support additional seats");
        }

        int currentSeats = sub.getSeatCount() != null ? sub.getSeatCount() : 0;
        if (newSeatCount == currentSeats) {
            throw new IllegalArgumentException("Seat count is unchanged");
        }

        BigDecimal planPrice = "yearly".equals(sub.getPlanDuration()) ? plan.getAnnualPrice() : plan.getMonthlyPrice();
        BigDecimal newMonthlyAmount = planPrice.add(seatCostFor(plan, sub.getPlanDuration(), newSeatCount));

        if (newSeatCount < currentSeats) {
            // Removing seats: can't drop below lawyers actually assigned; no refund,
            // takes effect at the next renewal (seatCount is stored now, but this
            // period was already paid at the old, higher amount)
            int usedSeats = accountRepository.countByCompanyUuidAndIsCompanyFalse(account.getCompanyUuid());
            if (newSeatCount < usedSeats) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Unassign a lawyer before removing that many seats (" + usedSeats + " currently assigned)");
            }
            sub.setSeatCount(newSeatCount);
            sub.setUpdatedAt(LocalDateTime.now());
            userSubscriptionRepository.save(sub);
            return UpdateSeatsResponse.builder()
                    .additionalSeats(newSeatCount)
                    .usedSeats(usedSeats)
                    .newMonthlyAmount(newMonthlyAmount)
                    .proRataCharge(BigDecimal.ZERO)
                    .message("Seats reduced. This takes effect from your next billing date; no refund for the current period.")
                    .build();
        }

        // Adding seats during a free trial: nothing to charge yet -- just raise the
        // count now, and it's priced correctly (seatPrice x seats) when the trial
        // converts to the first real charge (see startRenewalAttempt).
        if (Boolean.TRUE.equals(sub.getIsTrial())) {
            sub.setSeatCount(newSeatCount);
            sub.setUpdatedAt(LocalDateTime.now());
            userSubscriptionRepository.save(sub);
            return UpdateSeatsResponse.builder()
                    .additionalSeats(newSeatCount)
                    .usedSeats(accountRepository.countByCompanyUuidAndIsCompanyFalse(account.getCompanyUuid()))
                    .newMonthlyAmount(newMonthlyAmount)
                    .proRataCharge(BigDecimal.ZERO)
                    .message("Seat" + (newSeatCount - currentSeats > 1 ? "s" : "") + " added for free during your trial.")
                    .build();
        }

        // Adding seats: charge pro-rata now for the remainder of this billing cycle
        if (sub.getRenewsAt() == null || sub.getPaywayCustomerNumber() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription has no saved card to charge");
        }
        // lastPaidBillingPeriod whitelists INITIAL/RENEWAL invoice types, which also
        // correctly excludes VERIFICATION (trial card-check charges) without needing
        // to know about that type specifically -- cleaner than filtering it out by name.
        SubscriptionInvoice lastPaid = lastPaidBillingPeriod(sub)
                .orElseThrow(() -> new IllegalStateException("No paid invoice found for this subscription"));

        LocalDate periodStart = lastPaid.getPeriodStart();
        LocalDate periodEnd = sub.getRenewsAt().toLocalDate();
        LocalDate today = LocalDate.now(BILLING_ZONE);
        long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, periodEnd);
        if (daysInPeriod <= 0) daysInPeriod = 1;
        if (daysRemaining < 0) daysRemaining = 0;

        int seatsAdded = newSeatCount - currentSeats;
        // Full cost of the new seats for this whole billing period (annualized
        // for a yearly plan, same as seatCostFor elsewhere), then pro-rated down
        // to just the days remaining in it.
        BigDecimal fullSeatsCost = seatCostFor(plan, sub.getPlanDuration(), seatsAdded);
        BigDecimal proRataAmount = fullSeatsCost
                .multiply(BigDecimal.valueOf(daysRemaining))
                .divide(BigDecimal.valueOf(daysInPeriod), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal proRataGst = proRataAmount.multiply(GST_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal proRataTotal = proRataAmount.add(proRataGst);

        String customerNumber = sub.getPaywayCustomerNumber();
        SubscriptionInvoice invoice = invoiceRepository.saveAndFlush(SubscriptionInvoice.builder()
                .userUuid(userUuid)
                .userType(USER_TYPE_FIRM)
                .companyUuid(account.getCompanyUuid())
                .userSubscriptionUuid(sub.getUuid())
                .invoiceType(SubscriptionInvoiceType.SEAT_ADDITION)
                .planId(plan.getId())
                .planDuration(sub.getPlanDuration())
                .periodStart(today)
                .periodEnd(periodEnd)
                .dueDate(today)
                .amount(proRataAmount)
                .gstAmount(proRataGst)
                .totalAmount(proRataTotal)
                .seatCount(seatsAdded)
                .build());

        PayWayTransaction txn;
        try {
            txn = transactionRepository.saveAndFlush(PayWayTransaction.builder()
                    .invoiceUuid(invoice.getUuid())
                    .userUuid(userUuid)
                    .userType(USER_TYPE_FIRM)
                    .companyUuid(account.getCompanyUuid())
                    .planId(plan.getId())
                    .planDuration(sub.getPlanDuration())
                    .customerNumber(customerNumber)
                    .amount(proRataAmount)
                    .gstAmount(proRataGst)
                    .totalAmount(proRataTotal)
                    .seatCount(seatsAdded)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // uq_payway_transactions_one_pending: another payment by this payer is in flight
            invoice.setStatus(SubscriptionInvoiceStatus.VOID);
            invoiceRepository.save(invoice);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A payment is already in progress, please wait a moment");
        }

        try {
            PayWayTransactionResponse result = payWayClient.createTransaction(txn);
            applyResult(txn, result);
            transactionRepository.save(txn);
        } catch (RuntimeException e) {
            // Never leave the row PENDING: it would block this firm's next payments and renewals
            String reason = e instanceof ResponseStatusException rse ? rse.getReason() : "Unexpected error";
            txn.setStatus(PayWayTransactionStatus.ERROR);
            txn.setErrorMessage(reason);
            transactionRepository.save(txn);
            invoice.setStatus(SubscriptionInvoiceStatus.VOID);
            invoiceRepository.save(invoice);
            log.warn("❌ Seat payment {} failed: {}", txn.getOrderNumber(), reason, e);
            throw e;
        }

        if (txn.getStatus() != PayWayTransactionStatus.APPROVED) {
            invoice.setStatus(SubscriptionInvoiceStatus.VOID);
            invoiceRepository.save(invoice);
            String reason = txn.getErrorMessage() != null ? txn.getErrorMessage() : txn.getResponseText();
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment declined: " + reason);
        }

        invoice.setStatus(SubscriptionInvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        sub.setSeatCount(newSeatCount);
        sub.setUpdatedAt(LocalDateTime.now());
        userSubscriptionRepository.save(sub);

        return UpdateSeatsResponse.builder()
                .additionalSeats(newSeatCount)
                .usedSeats(accountRepository.countByCompanyUuidAndIsCompanyFalse(account.getCompanyUuid()))
                .newMonthlyAmount(newMonthlyAmount)
                .proRataCharge(proRataTotal)
                .invoiceNumber(invoice.getInvoiceNumber())
                .message("Seat" + (seatsAdded > 1 ? "s" : "") + " added, charged pro-rata for the rest of this cycle.")
                .build();
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
        // The card-verification charge at trial signup is refunded and isn't a real
        // bill -- never shown, regardless of status.
        if (invoice.getInvoiceType() == SubscriptionInvoiceType.VERIFICATION) {
            return false;
        }
        return switch (invoice.getStatus()) {
            case PAID, OVERDUE -> true;
            case DUE -> invoice.getInvoiceType() == SubscriptionInvoiceType.RENEWAL;
            case VOID, REFUNDED -> false;
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
                boolean wasTrial = Boolean.TRUE.equals(sub.getIsTrial());
                sub.setIsTrial(false); // this is always a real paid charge -- for a trial subscriber, their first one
                log.info("✅ Subscription {} renewed ({}), next billing {}{}", sub.getUuid(),
                        invoice.getInvoiceNumber(), sub.getRenewsAt(), wasTrial ? " (trial converted to paid)" : "");
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

        // Renewals -- including a trial subscriber's first real charge -- are always
        // computed fresh from the plan's current price and the subscription's current
        // seat count, never copied from a previous invoice. This matters even though a
        // trial subscriber DOES have a prior PAID invoice (the trial period itself,
        // see openTrialInvoice): that invoice's amount is $0, so blindly copying it
        // forward would charge $0 forever instead of the real plan price. This is also
        // how a seat count change (added or removed via Manage Seats) takes effect at
        // the next billing date, and how a plan price change in the DB applies from the
        // next renewal onward.
        Subscription plan = subscriptionRepository.findById(sub.getPlanId()).orElse(null);
        if (plan == null) {
            log.warn("⚠️ Subscription {} references missing plan {}, skipping renewal", sub.getUuid(), sub.getPlanId());
            return null;
        }
        BigDecimal planPrice = "yearly".equals(sub.getPlanDuration()) ? plan.getAnnualPrice() : plan.getMonthlyPrice();
        int seatCount = sub.getSeatCount() != null ? sub.getSeatCount() : 0;
        BigDecimal amount = planPrice.add(seatCostFor(plan, sub.getPlanDuration(), seatCount));
        BigDecimal gstAmount = amount.multiply(GST_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = amount.add(gstAmount);

        UUID companyUuid = USER_TYPE_FIRM.equals(sub.getUserType())
                ? accountRepository.findByUuid(sub.getUserUuid()).map(Account::getCompanyUuid).orElse(null)
                : null;

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
                    .companyUuid(companyUuid)
                    .userSubscriptionUuid(sub.getUuid())
                    .invoiceType(SubscriptionInvoiceType.RENEWAL)
                    .planId(sub.getPlanId())
                    .planDuration(sub.getPlanDuration())
                    .periodStart(periodStart)
                    .periodEnd(addPeriod(periodStart, sub.getPlanDuration()))
                    .dueDate(periodStart)
                    .amount(amount)
                    .gstAmount(gstAmount)
                    .totalAmount(totalAmount)
                    .seatCount(seatCount)
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
                .seatCount(invoice.getSeatCount())
                .currency(invoice.getCurrency())
                .build());
    }

    // The billing period renewals and seat additions are based on: the latest paid
    // INITIAL / RENEWAL invoice (a SEAT_ADDITION invoice starts mid-cycle, so it isn't one)
    private Optional<SubscriptionInvoice> lastPaidBillingPeriod(UserSubscription sub) {
        return invoiceRepository
                .findFirstByUserSubscriptionUuidAndStatusAndInvoiceTypeInAndDeletedAtIsNullOrderByPeriodStartDesc(
                        sub.getUuid(), SubscriptionInvoiceStatus.PAID,
                        List.of(SubscriptionInvoiceType.INITIAL, SubscriptionInvoiceType.RENEWAL));
    }

    private static LocalDate addPeriod(LocalDate from, String duration) {
        return "yearly".equals(duration) ? from.plusYears(1) : from.plusMonths(1);
    }

    // ---------------------------------------------------------------------
    // Trial reminder emails (TrialReminderJob)
    // ---------------------------------------------------------------------

    /**
     * Emails trial subscribers ahead of their first real charge, on the schedule
     * configured by an admin (system_settings.payments.trialReminderDaysBeforeCharge --
     * any number of day-offsets, not just a fixed "5 and 1"; defaults to [5, 1] if
     * unset). Skipped entirely if system_settings.payments.trialReminderEmailsEnabled
     * is false.
     */
    public void sendTrialReminders() {
        if (!isTrialReminderEmailsEnabled()) {
            log.info("📧 Trial reminder emails are disabled in system settings, skipping");
            return;
        }
        List<Integer> reminderDays = getConfiguredTrialReminderDays();
        LocalDate today = LocalDate.now(BILLING_ZONE);

        List<UserSubscription> trialSubs = userSubscriptionRepository
                .findByStatusAndIsTrialTrueAndDeletedAtIsNull(UserSubscription.STATUS_ACTIVE);
        log.info("📧 Checking {} trial subscription(s) against reminder schedule {}", trialSubs.size(), reminderDays);

        for (UserSubscription sub : trialSubs) {
            if (sub.getRenewsAt() == null) continue;
            LocalDate trialEnds = sub.getRenewsAt().toLocalDate();

            for (int daysBefore : reminderDays) {
                if (!trialEnds.equals(today.plusDays(daysBefore))) continue;
                try {
                    // Claim this (subscription, daysBefore) pair before sending -- the unique
                    // constraint on trial_reminder_sends means a second app instance racing
                    // the same cron tick gets a DataIntegrityViolationException here instead
                    // of also sending the email.
                    trialReminderSendRepository.saveAndFlush(TrialReminderSend.builder()
                            .userSubscriptionUuid(sub.getUuid())
                            .daysBefore(daysBefore)
                            .build());
                } catch (DataIntegrityViolationException e) {
                    continue; // already sent (by this run or another instance)
                }
                try {
                    sendTrialReminderEmail(sub, daysBefore);
                } catch (Exception e) {
                    // The reminder is already claimed above -- a failed send here is
                    // logged and not retried, rather than risking a duplicate email.
                    log.error("❌ Trial reminder ({} day(s) before) failed for user_subscription {}",
                            daysBefore, sub.getUuid(), e);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // system_settings.payments (admin-configurable)
    // ---------------------------------------------------------------------

    private JsonNode readPaymentsSettings() {
        return systemSettingRepository.findFirstByRemovedAtIsNull()
                .map(SystemSetting::getSettings)
                .map(s -> s.get("payments"))
                .orElse(null);
    }

    private boolean isTrialReminderEmailsEnabled() {
        JsonNode payments = readPaymentsSettings();
        if (payments == null) return true;
        JsonNode value = payments.path("trialReminderEmailsEnabled").path("value");
        return !value.isBoolean() || value.asBoolean();
    }

    private List<Integer> getConfiguredTrialReminderDays() {
        JsonNode payments = readPaymentsSettings();
        if (payments == null) return DEFAULT_TRIAL_REMINDER_DAYS;
        JsonNode value = payments.path("trialReminderDaysBeforeCharge").path("value");
        if (!value.isArray() || value.isEmpty()) return DEFAULT_TRIAL_REMINDER_DAYS;
        List<Integer> days = new java.util.ArrayList<>();
        value.forEach(n -> {
            if (n.isNumber() && n.asInt() > 0) days.add(n.asInt());
        });
        return days.isEmpty() ? DEFAULT_TRIAL_REMINDER_DAYS : days;
    }

    /** Admin-configurable override (system_settings.payments.verificationAmount); falls back to PAYWAY_VERIFICATION_AMOUNT. */
    private BigDecimal getEffectiveVerificationAmount() {
        JsonNode payments = readPaymentsSettings();
        if (payments != null) {
            JsonNode value = payments.path("verificationAmount").path("value");
            if (value.isNumber()) {
                return BigDecimal.valueOf(value.asDouble()).setScale(2, java.math.RoundingMode.HALF_UP);
            }
        }
        return payWayProperties.getVerificationAmount();
    }

    private void sendTrialReminderEmail(UserSubscription sub, int daysLeft) {
        Account account = accountRepository.findByUuid(sub.getUserUuid()).orElse(null);
        if (account == null || account.getEmail() == null || account.getEmail().isBlank()) {
            return;
        }

        Subscription plan = subscriptionRepository.findById(sub.getPlanId()).orElse(null);
        String planName = plan != null ? plan.getPlanName() : "your plan";
        String cycleLabel = "yearly".equals(sub.getPlanDuration()) ? "year" : "month";

        String amountText = "the plan price";
        if (plan != null) {
            BigDecimal planPrice = "yearly".equals(sub.getPlanDuration()) ? plan.getAnnualPrice() : plan.getMonthlyPrice();
            int seatCount = sub.getSeatCount() != null ? sub.getSeatCount() : 0;
            BigDecimal amount = planPrice.add(seatCostFor(plan, sub.getPlanDuration(), seatCount));
            BigDecimal gst = amount.multiply(GST_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
            amountText = "$" + amount.add(gst).setScale(2, java.math.RoundingMode.HALF_UP) + " AUD";
        }

        String chargeDate = sub.getRenewsAt().toLocalDate().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH));
        String subject = daysLeft == 1
                ? "Your Boss Justice free trial ends tomorrow"
                : "Your Boss Justice free trial ends in " + daysLeft + " days";

        String bodyHtml =
                "<p>Hello,</p>" +
                "<p>Your free trial of <strong>" + planName + "</strong> ends on <strong>" + chargeDate + "</strong>.</p>" +
                "<p>After that, your saved card will be charged <strong>" + amountText + "</strong> automatically" +
                (daysLeft == 1 ? " tomorrow" : "") + ", and your subscription will then renew every " + cycleLabel +
                ".</p>" +
                "<p>If you'd like to cancel, you can do so any time before the trial ends from your subscription " +
                "settings -- no charge will be made if you cancel before then.</p>" +
                "<p>Thanks,<br>Boss Justice</p>";

        emailService.sendEmail(account.getEmail(), subject, bodyHtml);
        log.info("📧 Trial reminder ({} day(s) left) sent to {} for subscription {}", daysLeft, account.getEmail(), sub.getUuid());
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
        sub.setSeatCount(txn.getSeatCount());
        sub.setIsTrial(false); // this path charges in full today -- never a trial
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
     * Like activateSubscription, but for a trial signup: access starts now, free,
     * with the first real charge deferred to renews_at (= the trial's end date).
     */
    private UserSubscription activateTrialSubscription(PayWayTransaction verificationTxn, SubscriptionInvoice trialInvoice) {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = userSubscriptionRepository
                .findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(verificationTxn.getUserUuid())
                .orElseGet(() -> UserSubscription.builder()
                        .uuid(UUID.randomUUID())
                        .userUuid(verificationTxn.getUserUuid())
                        .createdAt(now)
                        .build());

        sub.setUserType(verificationTxn.getUserType());
        sub.setPlanId(verificationTxn.getPlanId());
        sub.setPlanDuration(verificationTxn.getPlanDuration());
        sub.setStatus(UserSubscription.STATUS_ACTIVE);
        sub.setSeatCount(verificationTxn.getSeatCount());
        sub.setIsTrial(true);
        sub.setHasUsedTrial(true);
        sub.setCancelledAt(null);
        sub.setCancelReason(null);
        if (sub.getStartDate() == null) {
            sub.setStartDate(now); // member since
        }
        sub.setRenewsAt(trialInvoice.getPeriodEnd().atStartOfDay()); // trial end = first real charge date
        sub.setPaywayCustomerNumber(verificationTxn.getCustomerNumber());
        sub.setUpdatedAt(now);

        sub = userSubscriptionRepository.save(sub);
        log.info("🆓 Subscription {} on trial for {} until {}", sub.getUuid(), sub.getUserUuid(), sub.getRenewsAt());
        return sub;
    }

    /**
     * The verification charge invoice: a tiny, immediately-refunded charge used to
     * confirm the card is real before starting a free trial. Never a real bill --
     * excluded from billing history regardless of status (isBillingHistoryInvoice).
     */
    private SubscriptionInvoice openVerificationInvoice(UUID userUuid, String userType, UUID companyUuid,
                                                         Long planId, String duration, BigDecimal verificationAmount) {
        LocalDate today = LocalDate.now(BILLING_ZONE);
        SubscriptionInvoice invoice = SubscriptionInvoice.builder()
                .userUuid(userUuid)
                .userType(userType)
                .companyUuid(companyUuid)
                .invoiceType(SubscriptionInvoiceType.VERIFICATION)
                .planId(planId)
                .planDuration(duration)
                .amount(verificationAmount)
                .gstAmount(BigDecimal.ZERO)
                .totalAmount(verificationAmount)
                .build();
        invoice.setPeriodStart(today);
        invoice.setPeriodEnd(today);
        invoice.setDueDate(today);
        return invoiceRepository.saveAndFlush(invoice);
    }

    /**
     * The trial period itself: amount 0, settled as PAID immediately (nothing was
     * actually charged for the subscription -- only the separate, refunded
     * verification charge was). periodEnd is the date the first real charge is due.
     */
    private SubscriptionInvoice openTrialInvoice(UUID userUuid, String userType, UUID companyUuid, Subscription plan,
                                                 String duration, int additionalSeats, int trialDays) {
        LocalDate today = LocalDate.now(BILLING_ZONE);
        SubscriptionInvoice invoice = SubscriptionInvoice.builder()
                .userUuid(userUuid)
                .userType(userType)
                .companyUuid(companyUuid)
                .invoiceType(SubscriptionInvoiceType.INITIAL)
                .planId(plan.getId())
                .planDuration(duration)
                .amount(BigDecimal.ZERO)
                .gstAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .seatCount(additionalSeats)
                .status(SubscriptionInvoiceStatus.PAID)
                .build();
        invoice.setPeriodStart(today);
        invoice.setPeriodEnd(today.plusDays(trialDays));
        invoice.setDueDate(today);
        invoice.setPaidAt(LocalDateTime.now());
        return invoiceRepository.saveAndFlush(invoice);
    }

    /**
     * Starts a free trial instead of charging today: saves the card, runs a small
     * refundable verification charge to confirm it's real, then activates the
     * subscription for free with the first real charge deferred to renews_at.
     * A declined verification behaves exactly like a declined full payment (402,
     * no subscription created). A failed refund never blocks the trial -- the
     * card is already proven valid regardless.
     */
    private PayWayPaymentResponse startTrialSubscription(UUID userUuid, Account account, Subscription plan,
            String duration, MakePaymentRequest request, int additionalSeats, String userType, UUID companyUuid,
            String customerNumber, int trialDays) {

        BigDecimal verificationAmount = getEffectiveVerificationAmount();

        PayWayTransaction txn;
        try {
            txn = transactionTemplate.execute(status -> {
                userSubscriptionRepository.findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(userUuid)
                        .filter(sub -> sub.getStatus() == UserSubscription.STATUS_ACTIVE)
                        .ifPresent(sub -> {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                    "You already have an active subscription");
                        });

                SubscriptionInvoice verificationInvoice = openVerificationInvoice(
                        userUuid, userType, companyUuid, plan.getId(), duration, verificationAmount);
                return transactionRepository.saveAndFlush(PayWayTransaction.builder()
                        .invoiceUuid(verificationInvoice.getUuid())
                        .userUuid(userUuid)
                        .userType(userType)
                        .companyUuid(companyUuid)
                        .planId(plan.getId())
                        .planDuration(duration)
                        .customerNumber(customerNumber)
                        .amount(verificationAmount)
                        .gstAmount(BigDecimal.ZERO)
                        .totalAmount(verificationAmount)
                        .seatCount(additionalSeats)
                        .build());
            });
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A payment is already in progress, please wait a moment");
        }
        log.info("🆓 Trial signup {} started by {}: verifying card with {} {}", txn.getOrderNumber(), userUuid,
                txn.getTotalAmount(), txn.getCurrency());

        PayWayTransactionResponse result;
        try {
            payWayClient.saveCustomer(txn, request.getSingleUseTokenId(), payerName(account), account.getEmail());
            result = payWayClient.createTransaction(txn);
        } catch (RuntimeException e) {
            String reason = e instanceof ResponseStatusException rse ? rse.getReason() : "Unexpected error";
            txn.setStatus(PayWayTransactionStatus.ERROR);
            txn.setErrorMessage(reason);
            transactionRepository.save(txn);
            log.warn("❌ Trial signup {} failed: {}", txn.getOrderNumber(), reason, e);
            throw e;
        }
        applyResult(txn, result);

        if (txn.getStatus() != PayWayTransactionStatus.APPROVED) {
            // Card verification declined -- no trial, no subscription, same as a declined full charge
            PayWayTransaction declinedTxn = txn;
            transactionTemplate.executeWithoutResult(status -> {
                transactionRepository.save(declinedTxn);
                SubscriptionInvoice inv = invoiceRepository.findByUuid(declinedTxn.getInvoiceUuid()).orElseThrow();
                inv.setStatus(SubscriptionInvoiceStatus.VOID);
                invoiceRepository.save(inv);
            });
            log.info("💳 Trial signup {} card verification {}: {} {}", txn.getOrderNumber(), txn.getStatus(),
                    result.getResponseCode(), result.getResponseText());
            return toResponse(txn, invoiceRepository.findByUuid(txn.getInvoiceUuid()).orElseThrow(), null);
        }

        // Verification approved -- refund it right away. A failed refund is logged
        // but never blocks the trial: the card is already proven valid either way.
        transactionRepository.save(txn);
        boolean refunded = false;
        try {
            PayWayTransactionResponse refundResult = payWayClient.refundTransaction(txn, verificationAmount);
            if (refundResult.isApproved()) {
                txn.setRefundedAt(LocalDateTime.now());
                txn.setRefundTransactionId(refundResult.getTransactionId());
                refunded = true;
                log.info("↩️ Trial signup {} verification charge refunded (refund txn {})",
                        txn.getOrderNumber(), refundResult.getTransactionId());
            } else {
                log.warn("⚠️ Trial signup {} verification refund not approved ({}), proceeding with trial anyway",
                        txn.getOrderNumber(), refundResult.getStatus());
            }
        } catch (RuntimeException e) {
            log.warn("⚠️ Trial signup {} verification refund failed, proceeding with trial anyway: {}",
                    txn.getOrderNumber(), e.getMessage());
        }

        PayWayTransaction savedTxn = txn;
        boolean finalRefunded = refunded;
        Outcome outcome = transactionTemplate.execute(status -> {
            transactionRepository.save(savedTxn);
            SubscriptionInvoice verificationInvoice = invoiceRepository.findByUuid(savedTxn.getInvoiceUuid()).orElseThrow();
            verificationInvoice.setStatus(finalRefunded ? SubscriptionInvoiceStatus.REFUNDED : SubscriptionInvoiceStatus.PAID);
            verificationInvoice.setPaidAt(LocalDateTime.now());
            invoiceRepository.save(verificationInvoice);

            SubscriptionInvoice trialInvoice = openTrialInvoice(userUuid, userType, companyUuid, plan, duration, additionalSeats, trialDays);
            UserSubscription sub = activateTrialSubscription(savedTxn, trialInvoice);
            trialInvoice.setUserSubscriptionUuid(sub.getUuid());
            trialInvoice = invoiceRepository.save(trialInvoice);
            return new Outcome(trialInvoice, sub);
        });

        log.info("🆓 Trial started for {}: {} {}, first charge {} on {}", userUuid, plan.getPlanName(), duration,
                request.getTotalAmount(), outcome.subscription().getRenewsAt());

        PayWayPaymentResponse response = toResponse(txn, outcome.invoice(), outcome.subscription());
        response.setTrial(true);
        response.setTrialEndsAt(outcome.subscription().getRenewsAt());
        response.setVerificationRefunded(finalRefunded);
        response.setRenewalAmount(request.getAmount());
        response.setRenewalGstAmount(request.getGstAmount());
        response.setRenewalTotalAmount(request.getTotalAmount());
        return response;
    }

    /**
     * The DUE first-payment invoice for this plan: reused when the user retries
     * (e.g. after a decline) so billing history doesn't fill up with duplicates,
     * with its period moved to start today. Open first-payment invoices for a
     * different plan / amount are voided.
     */
    private SubscriptionInvoice openInitialInvoice(UUID userUuid, String userType, UUID companyUuid,
                                                   Subscription plan, String duration, MakePaymentRequest request,
                                                   int additionalSeats) {
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
                .seatCount(additionalSeats)
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
