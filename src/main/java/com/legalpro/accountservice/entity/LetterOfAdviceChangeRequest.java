package com.legalpro.accountservice.entity;

import com.legalpro.accountservice.enums.LetterOfAdviceChangeCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "letter_of_advice_change_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfAdviceChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Builder.Default is required -- without it the generated builder
    // ignores this initializer and leaves uuid null.
    @Builder.Default
    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "document_uuid", nullable = false)
    private UUID documentUuid;

    @Column(name = "client_uuid", nullable = false)
    private UUID clientUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LetterOfAdviceChangeCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
