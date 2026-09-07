package com.legalpro.accountservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final long accessTokenExpirationMs = 24 * 60 * 60 * 1000;       // 1 day
    private final long refreshTokenExpirationMs = 30L * 24 * 60 * 60 * 1000; // 30 days

    private final Key key;

    public JwtUtil() {
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable is not set. A hardcoded/fallback secret is " +
                            "not acceptable here -- refusing to start rather than sign tokens with a " +
                            "predictable key.");
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    // --- Generate Access Token ---
    public String generateAccessToken(UUID uuid, String username, Collection<? extends GrantedAuthority> authorities, boolean subscribed, boolean isCompany,
                                      boolean isCompanyMember,
                                      String companyName) {
        return generateToken(uuid, username, authorities, subscribed, isCompany, isCompanyMember, companyName,
                accessTokenExpirationMs, UUID.randomUUID().toString());
    }

    // --- Generate Refresh Token. Caller supplies the jti so it can persist a
    // RefreshSession row keyed by the same id before/after this returns. ---
    public String generateRefreshToken(UUID uuid, String username, Collection<? extends GrantedAuthority> authorities, boolean subscribed, boolean isCompany,
                                       boolean isCompanyMember,
                                       String companyName, String jti) {
        return generateToken(uuid, username, authorities, subscribed, isCompany, isCompanyMember, companyName,
                refreshTokenExpirationMs, jti);
    }

    // --- Core token generator (now includes uuid + jti) ---
    private String generateToken(UUID uuid, String username, Collection<? extends GrantedAuthority> authorities, boolean subscribed, boolean isCompany,
                                 boolean isCompanyMember,
                                 String companyName, long expirationMs, String jti) {
        Set<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setId(jti)
                .setSubject(username)                   // email as subject
                .claim("uuid", uuid.toString())         // ✅ add uuid claim
                .claim("roles", roles)
                .claim("Subscribed", subscribed)
                .claim("isCompany", isCompany)
                .claim("isCompanyMember", isCompanyMember)
                .claim("companyName", companyName)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // --- Extract username (email) ---
    public String getUsernameFromJwt(String token) {
        return parseClaims(token).getSubject();
    }

    // --- Extract uuid ---
    public UUID getUuidFromJwt(String token) {
        String uuidStr = parseClaims(token).get("uuid", String.class);
        return uuidStr != null ? UUID.fromString(uuidStr) : null;
    }

    // --- Extract roles as GrantedAuthorities ---
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> getRolesFromJwt(String token) {
        List<String> roles = parseClaims(token).get("roles", List.class);
        return roles != null
                ? roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                : Collections.emptyList();
    }

    // --- Extract raw role names (for JwtAuthorizationFilter) ---
    @SuppressWarnings("unchecked")
    public Collection<String> getRoleNamesFromJwt(String token) {
        return parseClaims(token).get("roles", List.class);
    }

    // --- Validate token ---
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // --- Internal claims parser ---
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Key getKey() {
        return this.key;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Date getExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }
}
