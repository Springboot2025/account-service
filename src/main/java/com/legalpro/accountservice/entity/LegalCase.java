package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "case_number", nullable = false, unique = true, length = 128)
    private String caseNumber;

    @Column(length = 255)
    private String listing; // short title or case name

    private LocalDate courtDate;

    @Column(name = "available_trust_funds", precision = 19, scale = 2)
    private BigDecimal availableTrustFunds;

    @Column(length = 255)
    private String followUp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private CaseStatus status;

    @Column(nullable = false)
    private UUID clientUuid;

    @Column(nullable = false)
    private UUID lawyerUuid;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
