# PayWay — Code Walkthrough

For backend developers working on the PayWay subscription code. Flow and API contract: [PAYWAY_INTEGRATION.md](PAYWAY_INTEGRATION.md).

Base package: `src/main/java/com/legalpro/accountservice` (paths below are relative to it).

---

## 1. Map

```
controller/PayWayPaymentController        POST /api/payway/payments, GET /api/payway/invoices
        │
service/PayWayPaymentService              business flow: validate → invoice → payment row → PayWay → DB
        │            ▲
        │            └── service/SubscriptionRenewalJob   daily cron → renewDueSubscriptions()
        ▼
service/PayWayClient                      HTTP to PayWay (secret key, retries, pending polling, error mapping)
        │
service/PayWayAuditLogger                 masks and writes every call to payway_api_logs
```

| Layer | Files |
|---|---|
| Config | `config/PayWayProperties` (bound from `payway.*` in `application.yml` ← `PAYWAY_*` env vars) |
| Controller | `controller/PayWayPaymentController` |
| Services | `service/PayWayPaymentService`, `service/PayWayClient`, `service/PayWayAuditLogger`, `service/SubscriptionRenewalJob` |
| Entities | `entity/SubscriptionInvoice`, `entity/PayWayTransaction`, `entity/PayWayApiLog`, `entity/UserSubscription` (existing, + `paywayCustomerNumber`) |
| Repositories | `SubscriptionInvoiceRepository`, `PayWayTransactionRepository`, `PayWayApiLogRepository`, `UserSubscriptionRepository` (existing, + 2 queries) |
| Enums | `PayWayTransactionStatus`, `SubscriptionInvoiceStatus`, `SubscriptionInvoiceType` |
| DTOs (`dto/payway`) | Our API: `MakePaymentRequest`, `PayWayPaymentResponse`, `BillingHistoryItemDto`. PayWay's responses: `PayWayTransactionResponse`, `PayWayCustomerResponse`, `PayWayPaymentSetup`, `PayWayCreditCard` |
| Errors | `exception/PayWayException` (extends `ResponseStatusException`, so `GlobalExceptionHandler` returns its status and message) |
| Migration | `src/main/resources/db/migration/V76__create_payway_transactions.sql` |

---

## 2. Make a payment — `PayWayPaymentService.makePayment`

[PayWayPaymentService.java:83](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L83)

1. **Payer** — `loadPayer` ([:509](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L509)) loads the account from the JWT uuid. A lawyer with `companyUuid` but `isCompany = false` (firm member) → `403`.
2. **Validate** — plan exists; `amount` equals the plan's `monthly_price` / `annual_price`; `totalAmount == amount + gstAmount`. Otherwise `IllegalArgumentException` → `400`.
3. **Customer number** — `customerNumberFor` ([:522](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L522)): `FIRM-<companies.id>` for a firm admin, `LAW-<accounts.id>` otherwise.
4. **DB transaction 1** (`TransactionTemplate`):
   - `409` if the payer's `user_subscriptions` row is active.
   - `openInitialInvoice` ([:446](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L446)): reuses the payer's `DUE` INITIAL invoice for the same plan/amount (retry after a decline), voids others, sets the period to today → +1 month/year (Melbourne date).
   - Inserts the `PENDING` `payway_transactions` row. The DB fills `order_number` (`@Generated`), the entity fills `uuid` and `idempotency_key`.
   - A second `PENDING` row for the same payer violates `uq_payway_transactions_one_pending` → `DataIntegrityViolationException` → `409` (double click).
5. **PayWay** (outside any DB transaction, so a slow gateway doesn't hold a connection):
   - `payWayClient.saveCustomer` — `PUT /customers/{n}` with the token, name (`payerName`, [:532](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L532)) and email.
   - `payWayClient.createTransaction` — `POST /transactions` by customer number.
   - Any exception → the row is saved as `ERROR` (never left `PENDING`) and rethrown.
6. **DB transaction 2** — `applyResult` ([:483](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L483)) copies PayWay's response onto the row (`approved*` → `APPROVED`, `pending` → `ERROR`, else `DECLINED`). If approved: `activateSubscription` ([:414](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L414)) sets the user_subscriptions row active (`renews_at = invoice.period_end`, `payway_customer_number`, `start_date` only the first time), and the invoice becomes `PAID` and is linked to the subscription.
7. The controller maps `status` to HTTP: `APPROVED` → 200, `DECLINED` → 402, otherwise 502.

---

## 3. Renewals — `renewDueSubscriptions`

[SubscriptionRenewalJob.java](../../src/main/java/com/legalpro/accountservice/service/SubscriptionRenewalJob.java) runs on `payway.renewal-cron` (default 02:00 `Australia/Melbourne`) and does nothing unless `payway.auto-renew-enabled` is true and PayWay is configured.

[PayWayPaymentService.java:251](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L251) — for each active subscription with `renews_at <= now (Melbourne)`, `renewSubscription` ([:266](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L266)), errors isolated per subscription:

1. `startRenewalAttempt` ([:331](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L331)) in one DB transaction:
   - re-checks the subscription (another instance may have renewed it);
   - finds or creates the RENEWAL invoice for `period_start = renews_at` with the amounts of the last `PAID` invoice;
   - if that period is already `PAID`, corrects `renews_at` and stops;
   - skips if the invoice had an attempt in the last 20 h (one try per day, even with several instances);
   - inserts the `PENDING` payment row.
2. `createTransaction` — charge by customer number (no token).
3. Second DB transaction:
   - approved → invoice `PAID`, `renews_at = period_end`;
   - declined, or PayWay `422` → invoice `OVERDUE`, `failed_attempts + 1`; at 3 the subscription goes `status = 0`;
   - PayWay down / still pending → nothing counted, retried next run.

---

## 4. Billing history — `getBillingHistory`

[PayWayPaymentService.java:188](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L188) — the payer's invoices newest first, filtered by `isBillingHistoryInvoice` ([:211](../../src/main/java/com/legalpro/accountservice/service/PayWayPaymentService.java#L211)): `PAID`, `OVERDUE`, and `DUE` renewals. Plan names come from `subscriptions`; receipt/card fields from the invoice's `APPROVED` payment (one batch query).

---

## 5. PayWay client — `PayWayClient`

- Basic auth with the secret key on every request; form-encoded bodies.
- `saveCustomer` ([:76](../../src/main/java/com/legalpro/accountservice/service/PayWayClient.java#L76)) — `PUT /customers/{n}`, `sendEmailReceipts=false`, name truncated to 60 chars.
- `createTransaction` ([:105](../../src/main/java/com/legalpro/accountservice/service/PayWayClient.java#L105)) — `POST /transactions` with `Idempotency-Key = payway_transactions.idempotency_key`. If PayWay answers `pending`, polls `GET /transactions/{id}` every 3 s, up to 5 times.
- `send` ([:139](../../src/main/java/com/legalpro/accountservice/service/PayWayClient.java#L139)) — uses `RestClient.exchange` so the status and body are captured for the audit log even on errors. Retries once (after 2 s) on timeout, `429` or `503` — same idempotency key, so no double charge. Every attempt is audited.
- `parse` ([:184](../../src/main/java/com/legalpro/accountservice/service/PayWayClient.java#L184)) — `2xx` → DTO; `422` → `PayWayException(422, PayWay's field messages)`; `401/403` → `502` "not configured correctly"; anything else → `502`.

---

## 6. Audit log — `PayWayAuditLogger`

[PayWayAuditLogger.java:44](../../src/main/java/com/legalpro/accountservice/service/PayWayAuditLogger.java#L44) — one `payway_api_logs` row per HTTP attempt, linked to the payment (`payway_transaction_uuid`) and payer.

- Headers: only `Content-Type`, `Accept`, `Idempotency-Key` are kept — `Authorization` (secret key) is never stored.
- Request / response bodies: `cvn`, `accountNumber`, `bsb` removed; `cardNumber` removed unless already masked by PayWay (`456471...004`); `singleUseTokenId` → `****<last 4>`. Non-JSON responses are stored as `{ "raw": "<first 2000 chars>" }`.
- Write failures are logged and swallowed — auditing never breaks a payment.

To add a new sensitive field, add it to `REMOVED_KEYS` or `MASKED_KEYS`.

---

## 7. Database notes

- `order_number` and `invoice_number` come from DB functions `next_payway_order_number()` / `next_subscription_invoice_number()` (sequence + Melbourne date). Entities map them with `@Generated(event = INSERT)`, `insertable = false`. Rolled-back inserts leave gaps in the sequence — expected.
- `uq_payway_transactions_one_pending` (partial unique index) = at most one `PENDING` payment per payer.
- Billing dates (`period_*`, `renews_at`) are Melbourne dates; record timestamps (`created_at`, `paid_at`) use server time like the rest of the codebase. Cloud Run runs on UTC — compare billing dates with `LocalDateTime.now(BILLING_ZONE)`.

---

## 8. Common changes

| Change | Where |
|---|---|
| Number of renewal retries | `MAX_RENEWAL_ATTEMPTS` in `PayWayPaymentService` |
| Renewal time | `PAYWAY_RENEWAL_CRON` env var |
| Who may pay | `loadPayer` |
| Customer number format | `customerNumberFor` (PayWay max 20 chars) |
| Order / invoice number format | the two DB functions in `V76` (new migration — never edit an applied one) |
| What billing history shows | `isBillingHistoryInvoice` |

---

## 9. Running locally

1. `.env` needs `PAYWAY_SECRET_KEY`, `PAYWAY_PUBLISHABLE_KEY`, `PAYWAY_MERCHANT_ID=TEST` (sandbox), plus the usual app settings (see `LOCAL_DEV_SETUP.md`).
2. To test renewals: `PAYWAY_RENEWAL_AUTO_ENABLED=true` and e.g. `PAYWAY_RENEWAL_CRON="*/30 * * * * *"`, then set a subscription's `renews_at` to today.
3. If Flyway reports two migrations with version 76, run `./mvnw clean` (stale build output from an older V76).
4. Check results in `payway_transactions`, `subscription_invoices`, `payway_api_logs`, and in the PayWay portal (search by order number or customer number).
