package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userUuid; // The account this device belongs to

    @Column(nullable = false, unique = true, length = 255)
    private String deviceId; // Your app/device identifier

    @Column(nullable = false, unique = true, length = 500)
    private String fcmToken; // Firebase token for push notifications

    @Column(nullable = false)
    private String platform; // Android / iOS / Web

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
