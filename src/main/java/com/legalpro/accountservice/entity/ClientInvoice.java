package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "case_uuid", nullable = false)
    private UUID caseUuid;

    @Column(name = "lawyer_uuid", nullable = false)
    private UUID lawyerUuid; // logged-in user's UUID (from Account)

    @Column(name = "trust_balance", precision = 12, scale = 2)
    private BigDecimal trustBalance;

    @Column(name = "amount_requested", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountRequested;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "last_activity")
    private String lastActivity;

    @Column(name = "status", length = 50)
    private String status = "PENDING";

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
