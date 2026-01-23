package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private UUID actorUuid;
    private String actorName;

    @Column(nullable = false, length = 50)
    private String activityType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private UUID caseUuid;
    private UUID clientUuid;
    private UUID lawyerUuid;
    private UUID referenceUuid;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
