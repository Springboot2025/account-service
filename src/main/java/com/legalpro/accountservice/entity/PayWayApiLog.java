package com.legalpro.accountservice.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit row for one HTTP call to PayWay. Append-only; request/response
 * values are masked before saving (see PayWayAuditLogger).
 */
@Entity
@Table(name = "payway_api_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayWayApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "payway_transaction_uuid")
    private UUID paywayTransactionUuid;

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(nullable = false)
    private String operation;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(nullable = false)
    private String endpoint;

    @Builder.Default
    @Column(nullable = false)
    private Integer attempt = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    private JsonNode requestHeaders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body", columnDefinition = "jsonb")
    private JsonNode requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private JsonNode responseBody;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (uuid == null) uuid = UUID.randomUUID();
        createdAt = LocalDateTime.now();
    }
}
