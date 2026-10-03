package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.legalpro.accountservice.entity.PayWayApiLog;
import com.legalpro.accountservice.repository.PayWayApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Writes one payway_api_logs row per HTTP call to PayWay, masking sensitive
 * values first:
 *   never stored : Authorization header, cardNumber (unless PayWay already masked it), cvn, accountNumber
 *   masked       : singleUseTokenId -> ****<last 4>
 * A failure to write the audit row is logged and never breaks the payment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayWayAuditLogger {

    private static final Set<String> REMOVED_KEYS = Set.of("cvn", "accountNumber", "bsb");
    private static final Set<String> MASKED_KEYS = Set.of("singleUseTokenId");
    private static final Set<String> LOGGED_HEADERS = Set.of("Content-Type", "Accept", "Idempotency-Key");
    private static final int MAX_RAW_BODY = 2000;

    private final PayWayApiLogRepository repository;
    private final ObjectMapper objectMapper;

    /** Who/what a PayWay call belongs to, for linking audit rows. */
    public record Context(UUID paywayTransactionUuid, UUID userUuid) {
    }

    public void log(Context context, String operation, String httpMethod, String endpoint, int attempt,
                    Map<String, String> requestHeaders, MultiValueMap<String, String> requestForm,
                    Integer responseStatus, String responseBody, String errorMessage, long durationMs) {
        try {
            repository.save(PayWayApiLog.builder()
                    .paywayTransactionUuid(context != null ? context.paywayTransactionUuid() : null)
                    .userUuid(context != null ? context.userUuid() : null)
                    .operation(operation)
                    .httpMethod(httpMethod)
                    .endpoint(endpoint)
                    .attempt(attempt)
                    .requestHeaders(maskHeaders(requestHeaders))
                    .requestBody(maskForm(requestForm))
                    .responseStatus(responseStatus)
                    .responseBody(maskResponse(responseBody))
                    .errorMessage(errorMessage)
                    .durationMs((int) durationMs)
                    .build());
        } catch (Exception e) {
            log.warn("⚠️ Could not write PayWay audit log for {} {}: {}", httpMethod, endpoint, e.getMessage());
        }
    }

    private JsonNode maskHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        headers.forEach((name, value) -> {
            // Authorization (secret key) and anything unexpected are never stored
            if (LOGGED_HEADERS.contains(name)) {
                node.put(name, value);
            }
        });
        return node;
    }

    private JsonNode maskForm(MultiValueMap<String, String> form) {
        if (form == null || form.isEmpty()) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        form.forEach((key, values) -> {
            String value = values.isEmpty() ? null : values.get(0);
            if (REMOVED_KEYS.contains(key) || ("cardNumber".equals(key) && !isPayWayMasked(value))) {
                return;
            }
            node.put(key, MASKED_KEYS.contains(key) ? maskValue(value) : value);
        });
        return node;
    }

    private JsonNode maskResponse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            maskJson(node);
            return node;
        } catch (Exception e) {
            // Not JSON (e.g. an HTML error page): keep a short excerpt
            ObjectNode raw = objectMapper.createObjectNode();
            raw.put("raw", body.length() > MAX_RAW_BODY ? body.substring(0, MAX_RAW_BODY) : body);
            return raw;
        }
    }

    private void maskJson(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            List<String> keys = new ArrayList<>();
            obj.fieldNames().forEachRemaining(keys::add);
            for (String key : keys) {
                JsonNode value = obj.get(key);
                if (REMOVED_KEYS.contains(key)
                        || ("cardNumber".equals(key) && !isPayWayMasked(value.asText(null)))) {
                    obj.remove(key);
                } else if (MASKED_KEYS.contains(key) && value.isTextual()) {
                    obj.put(key, maskValue(value.asText()));
                } else {
                    maskJson(value);
                }
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(this::maskJson);
        }
    }

    // PayWay returns card numbers already masked, e.g. 456471...004
    private static boolean isPayWayMasked(String value) {
        return value != null && value.contains("...");
    }

    private static String maskValue(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }
}
