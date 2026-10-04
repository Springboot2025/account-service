package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.config.PayWayProperties;
import com.legalpro.accountservice.dto.payway.PayWayCustomerResponse;
import com.legalpro.accountservice.dto.payway.PayWayPaymentSetup;
import com.legalpro.accountservice.dto.payway.PayWayTransactionResponse;
import com.legalpro.accountservice.entity.PayWayTransaction;
import com.legalpro.accountservice.exception.PayWayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calls the PayWay REST API (https://www.payway.com.au/docs/rest.html) with
 * the secret key. Every call is written to payway_api_logs (masked) by
 * PayWayAuditLogger, including retries.
 */
@Slf4j
@Service
public class PayWayClient {

    // PayWay asks for a pause before retrying a 429/503
    private static final long RETRY_DELAY_MS = 2000;
    private static final int PENDING_POLL_ATTEMPTS = 5;
    private static final long PENDING_POLL_DELAY_MS = 3000;

    private final PayWayProperties properties;
    private final ObjectMapper objectMapper;
    private final PayWayAuditLogger auditLogger;
    private final RestClient restClient;

    public PayWayClient(PayWayProperties properties, ObjectMapper objectMapper, PayWayAuditLogger auditLogger) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.auditLogger = auditLogger;

        // JDK HttpClient: supports PATCH (needed for stop payments), unlike HttpURLConnection
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeaders(h -> {
                    if (properties.getSecretKey() != null) {
                        // PayWay: secret key as Basic username, blank password
                        h.setBasicAuth(properties.getSecretKey(), "");
                    }
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }

    private static final int MAX_CUSTOMER_NAME_LENGTH = 60;

    /**
     * PUT /customers/{customerNumber}: creates the customer (or replaces it if
     * the number already exists) with contact details and the card behind the
     * single-use token, saved in PayWay's customer vault. Uses up the token.
     */
    public PayWayCustomerResponse saveCustomer(PayWayTransaction txn, String singleUseTokenId,
                                               String customerName, String emailAddress) {
        ensureConfigured();
        PayWayAuditLogger.Context context = new PayWayAuditLogger.Context(txn.getUuid(), txn.getUserUuid());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("singleUseTokenId", singleUseTokenId);
        form.add("merchantId", properties.getMerchantId());
        if (customerName != null && !customerName.isBlank()) {
            form.add("customerName", customerName.length() <= MAX_CUSTOMER_NAME_LENGTH
                    ? customerName : customerName.substring(0, MAX_CUSTOMER_NAME_LENGTH));
        }
        if (emailAddress != null) {
            form.add("emailAddress", emailAddress);
        }
        // We send our own receipts
        form.add("sendEmailReceipts", "false");

        // PUT is idempotent by nature, so no Idempotency-Key is needed
        HttpResult result = send(context, "SAVE_CUSTOMER", HttpMethod.PUT,
                "/customers/" + txn.getCustomerNumber(), form, Map.of());
        return parse(result, PayWayCustomerResponse.class, "save customer " + txn.getCustomerNumber());
    }

    /**
     * PATCH /customers/{customerNumber}/payment-setup stopped=true ("stop all payments"):
     * PayWay rejects any further charge to this customer. Saving a new card later
     * (PUT /customers/{n}) clears the flag, so re-subscribing works as normal.
     */
    public PayWayPaymentSetup stopPayments(UUID userUuid, String customerNumber) {
        ensureConfigured();
        PayWayAuditLogger.Context context = new PayWayAuditLogger.Context(null, userUuid);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("stopped", "true");

        // PATCH with the same value is idempotent, so no Idempotency-Key is needed
        HttpResult result = send(context, "STOP_PAYMENTS", HttpMethod.PATCH,
                "/customers/" + customerNumber + "/payment-setup", form, Map.of());
        return parse(result, PayWayPaymentSetup.class, "stop payments " + customerNumber);
    }

    /**
     * POST /transactions: charges the card saved on the customer (no token).
     * A declined card is a normal response (status "declined"), not an exception.
     * If PayWay answers "pending" (slow card network), polls GET /transactions/{id}.
     */
    public PayWayTransactionResponse createTransaction(PayWayTransaction txn) {
        ensureConfigured();
        PayWayAuditLogger.Context context = new PayWayAuditLogger.Context(txn.getUuid(), txn.getUserUuid());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("transactionType", "payment");
        form.add("customerNumber", txn.getCustomerNumber());
        form.add("principalAmount", txn.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        form.add("currency", txn.getCurrency());
        form.add("orderNumber", txn.getOrderNumber());

        // Same key on a retry, so PayWay returns the original result instead of charging twice
        Map<String, String> headers = Map.of("Idempotency-Key", txn.getIdempotencyKey().toString());

        PayWayTransactionResponse result = parse(
                send(context, "CREATE_TRANSACTION", HttpMethod.POST, "/transactions", form, headers),
                PayWayTransactionResponse.class, "payment " + txn.getOrderNumber());

        for (int i = 0; i < PENDING_POLL_ATTEMPTS && result.isPending() && result.getTransactionId() != null; i++) {
            sleep(PENDING_POLL_DELAY_MS);
            log.info("⏳ PayWay transaction {} pending, checking again", result.getTransactionId());
            result = parse(send(context, "GET_TRANSACTION", HttpMethod.GET,
                    "/transactions/" + result.getTransactionId(), null, Map.of()),
                    PayWayTransactionResponse.class, "transaction " + result.getTransactionId());
        }
        return result;
    }

    // ---------------------------------------------------------------------

    private record HttpResult(int status, String body) {
    }

    /** Sends the request (retrying once on 429/503/timeout) and audits every attempt. */
    private HttpResult send(PayWayAuditLogger.Context context, String operation, HttpMethod method, String path,
                            MultiValueMap<String, String> form, Map<String, String> extraHeaders) {
        Map<String, String> loggedHeaders = new LinkedHashMap<>(extraHeaders);
        loggedHeaders.put("Accept", MediaType.APPLICATION_JSON_VALUE);
        if (form != null) {
            loggedHeaders.put("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        }

        for (int attempt = 1; ; attempt++) {
            long start = System.currentTimeMillis();
            try {
                RestClient.RequestBodySpec spec = restClient.method(method).uri(path);
                extraHeaders.forEach(spec::header);
                if (form != null) {
                    spec.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form);
                }
                HttpResult result = spec.exchange((request, response) -> new HttpResult(
                        response.getStatusCode().value(),
                        new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));

                auditLogger.log(context, operation, method.name(), path, attempt, loggedHeaders, form,
                        result.status(), result.body(), null, System.currentTimeMillis() - start);

                if ((result.status() == 429 || result.status() == 503) && attempt == 1) {
                    log.warn("⏳ PayWay busy ({}) on {} {}, retrying once", result.status(), method, path);
                    sleep(RETRY_DELAY_MS);
                    continue;
                }
                return result;
            } catch (ResourceAccessException e) {
                // Timeout / connection reset: we don't know if PayWay processed it,
                // so repeat the identical call (same Idempotency-Key) once.
                auditLogger.log(context, operation, method.name(), path, attempt, loggedHeaders, form,
                        null, null, e.getMessage(), System.currentTimeMillis() - start);
                if (attempt == 1) {
                    log.warn("⏳ PayWay unreachable on {} {}, retrying once: {}", method, path, e.getMessage());
                    sleep(RETRY_DELAY_MS);
                    continue;
                }
                log.error("❌ PayWay still unreachable on {} {}", method, path, e);
                throw new PayWayException(502, "Payment gateway is unavailable, please try again shortly", e);
            }
        }
    }

    private <T> T parse(HttpResult result, Class<T> type, String description) {
        int status = result.status();
        if (status >= 200 && status < 300) {
            try {
                return objectMapper.readValue(result.body(), type);
            } catch (Exception e) {
                log.error("❌ Unreadable PayWay response on {}", description, e);
                throw new PayWayException(502, "Unexpected response from payment gateway", e);
            }
        }
        if (status == 422) {
            // Validation errors are safe to show the user (expired token, bad card, ...)
            String message = extractValidationMessage(result.body());
            log.warn("⚠️ PayWay rejected {}: {}", description, message);
            throw new PayWayException(422, message);
        }
        if (status == 401 || status == 403) {
            log.error("❌ PayWay auth failed on {} -- check PAYWAY_SECRET_KEY / PAYWAY_MERCHANT_ID", description);
            throw new PayWayException(502, "Payment gateway is not configured correctly");
        }
        log.error("❌ PayWay error {} on {}", status, description);
        throw new PayWayException(502, "Payment gateway error, please try again");
    }

    // PayWay 422 body: {"data":[{"fieldName":"...","message":"...","fieldValue":"..."}]}
    private String extractValidationMessage(String body) {
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            List<String> messages = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode err : data) {
                    String msg = err.path("message").asText(null);
                    if (msg != null) messages.add(msg);
                }
            }
            if (!messages.isEmpty()) {
                return String.join("; ", messages);
            }
        } catch (Exception ignored) {
            // fall through to the generic message
        }
        return "Payment details were rejected by the gateway";
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new PayWayException(502, "Payment gateway call interrupted");
        }
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new PayWayException(503, "Payments are not available: PayWay is not configured");
        }
    }
}
