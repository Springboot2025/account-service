package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks every issued refresh token so a stolen-and-reused one can be
 * detected: each refresh token belongs to a "family" (the chain created by
 * repeated rotation from one login). Redeeming a refresh token marks it
 * `usedAt` and issues a new one in the same family. If a refresh token with
 * `usedAt` already set is presented again, that's a replay -- someone else
 * has a copy -- and the entire family is revoked.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

    @Id
    @Column(name = "jti", length = 64)
    private String jti;

    @Column(name = "family_id", nullable = false, length = 64)
    private String familyId;

    @Column(name = "user_uuid", nullable = false)
    private UUID userUuid;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder.Default
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;
}
