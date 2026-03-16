package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "user_uuid", nullable = false)
    private UUID userUuid;

    @Column(name = "user_type", nullable = false)
    private String userType; // INDIVIDUAL | FIRM

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Integer status = 0;
    // 0 = inactive
    // 1 = active
    // 2 = cancelled

    @Column(name = "plan_duration", nullable = false)
    private String planDuration;
    // monthly | yearly

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "renews_at")
    private LocalDateTime renewsAt;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}