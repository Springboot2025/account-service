package com.legalpro.accountservice.entity;

import com.legalpro.accountservice.enums.LetterOfAdviceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "letter_of_advice_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfAdviceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "lawyer_uuid", nullable = false)
    private UUID lawyerUuid;

    @Column(name = "client_uuid", nullable = false)
    private UUID clientUuid;

    @Column(name = "case_uuid", nullable = false)
    private UUID caseUuid;

    @Column(name = "template_uuid")
    private UUID templateUuid;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private LetterOfAdviceStatus status = LetterOfAdviceStatus.DRAFT;

    @Column(name = "lawyer_signature", columnDefinition = "TEXT")
    private String lawyerSignature;

    @Column(name = "lawyer_signed_at")
    private LocalDateTime lawyerSignedAt;

    @Column(name = "client_signature", columnDefinition = "TEXT")
    private String clientSignature;

    @Column(name = "client_signed_at")
    private LocalDateTime clientSignedAt;

    @Column(name = "sent_to_client_at")
    private LocalDateTime sentToClientAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
