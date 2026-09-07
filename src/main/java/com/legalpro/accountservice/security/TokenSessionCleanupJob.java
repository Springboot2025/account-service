package com.legalpro.accountservice.security;

import com.legalpro.accountservice.repository.RefreshSessionRepository;
import com.legalpro.accountservice.util.GcsUrlSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Both revoked_tokens and refresh_sessions accumulate one row per
 * logout/rotation forever unless something prunes expired ones -- this runs
 * daily so the tables don't grow unbounded. Also sweeps GcsUrlSigner's
 * in-memory signed-URL cache for the same reason.
 */
@Component
@RequiredArgsConstructor
public class TokenSessionCleanupJob {

    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshSessionRepository refreshSessionRepository;

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000L) // once a day
    @Transactional
    public void cleanup() {
        tokenBlacklistService.cleanupExpired();
        refreshSessionRepository.deleteExpiredBefore(LocalDateTime.now());
        GcsUrlSigner.evictExpiredCache();
    }
}
