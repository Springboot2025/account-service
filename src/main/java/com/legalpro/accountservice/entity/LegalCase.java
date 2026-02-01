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

    @Column(nullable = false)
    private UUID quoteUuid;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // inside LegalCase.java

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id")
    private CaseType caseType;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "case_priority", nullable = false)
    private Integer casePriority = 0;   // 0=normal, 1=high, 2=urgent

    @Column(name = "case_final_status", nullable = false)
    private Integer caseFinalStatus = 0; // 0=none, 1=won, 2=lost
}
