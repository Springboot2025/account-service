package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One reminder email actually sent for a trial subscription. The unique
 * (userSubscriptionUuid, daysBefore) pair both prevents a duplicate send and
 * lets the reminder schedule (system_settings.payments.trialReminderDaysBeforeCharge)
 * have any number of entries, not just a fixed "5 days" / "1 day" pair.
 */
@Entity
@Table(name = "trial_reminder_sends")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialReminderSend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_subscription_uuid", nullable = false)
    private UUID userSubscriptionUuid;

    @Column(name = "days_before", nullable = false)
    private Integer daysBefore;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }
}
