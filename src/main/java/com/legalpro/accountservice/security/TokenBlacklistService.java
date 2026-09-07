package com.legalpro.accountservice.security;

import com.legalpro.accountservice.entity.RevokedToken;
import com.legalpro.accountservice.repository.RevokedTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * DB-backed token revocation, keyed by JWT id (jti) rather than the raw
 * token string. Previously this was an in-memory ConcurrentHashMap, which
 * meant a token blacklisted via logout on one Cloud Run instance stayed
 * fully valid on every other instance for the rest of its lifetime -- a
 * distributed deployment silently defeated the whole point of logout.
 *
 * isBlacklisted() runs on every single authenticated request via
 * JwtAuthorizationFilter, and the overwhelming majority of calls resolve to
 * "false" -- paying a DB round-trip for that on every request is real,
 * measurable latency for something that's almost never true. A short-lived
 * in-memory cache sits in front of the DB check for that reason; a fresh
 * revocation still takes effect immediately because blacklistToken() writes
 * straight into the cache rather than waiting for it to expire.
 */
@Service
public class TokenBlacklistService {

    private static final long CACHE_TTL_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtUtil jwtUtil;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(boolean blacklisted, long expiresAtMillis) {
    }

    public TokenBlacklistService(RevokedTokenRepository revokedTokenRepository, JwtUtil jwtUtil) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    public void blacklistToken(String token, Date expiryDate) {
        if (token == null || expiryDate == null) return;
        String jti = jwtUtil.extractJti(token);
        if (jti == null) return; // token predates jti support; nothing to key on

        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiryDate.toInstant(), ZoneId.systemDefault());
        revokedTokenRepository.save(RevokedToken.builder().jti(jti).expiresAt(expiresAt).build());

        // Written synchronously so this exact revocation is enforced on this
        // instance immediately, not after the cache TTL elapses.
        cache.put(jti, new CacheEntry(true, Long.MAX_VALUE));
    }

    public boolean isBlacklisted(String token) {
        if (token == null) return false;
        String jti;
        try {
            // This runs before JwtAuthorizationFilter's own validateToken()
            // check, so an expired/malformed token must not throw here --
            // it isn't "blacklisted," it's just invalid, and the filter's
            // subsequent validation handles that case correctly on its own.
            jti = jwtUtil.extractJti(token);
        } catch (Exception e) {
            return false;
        }
        if (jti == null) return false;

        CacheEntry cached = cache.get(jti);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.blacklisted();
        }

        boolean blacklisted = revokedTokenRepository.findById(jti)
                .map(revoked -> {
                    if (revoked.getExpiresAt().isBefore(LocalDateTime.now())) {
                        // Expired anyway -- the token itself is no longer
                        // valid regardless, but clean up the row.
                        revokedTokenRepository.deleteById(jti);
                        return false;
                    }
                    return true;
                })
                .orElse(false);

        cache.put(jti, new CacheEntry(blacklisted, now + CACHE_TTL_MILLIS));
        return blacklisted;
    }

    /** Call periodically (e.g. from a scheduled job) to remove expired rows. */
    public void cleanupExpired() {
        revokedTokenRepository.deleteExpiredBefore(LocalDateTime.now());
        cache.clear();
    }
}
