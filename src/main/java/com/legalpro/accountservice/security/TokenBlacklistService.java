package com.legalpro.accountservice.security;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory blacklist. Stores token -> expiry date.
 * For production replace with Redis or other distributed store.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expiryDate) {
        if (token == null || expiryDate == null) return;
        blacklist.put(token, expiryDate);
    }

    public boolean isBlacklisted(String token) {
        if (token == null) return false;
        Date expiry = blacklist.get(token);
        if (expiry == null) return false;

        // Remove expired entries proactively
        if (expiry.before(new Date())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    // Optional: cleanup method you can call from a scheduled job to remove expired entries
    public void cleanupExpired() {
        Date now = new Date();
        blacklist.entrySet().removeIf(entry -> entry.getValue().before(now));
    }
}
