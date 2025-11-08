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

    private final String jwtSecret = "super_secure_secret_key_for_jwt_1234567890";

    private final long accessTokenExpirationMs = 24 * 60 * 60 * 1000;       // 1 day
    private final long refreshTokenExpirationMs = 30L * 24 * 60 * 60 * 1000; // 30 days

    private final Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

    // --- Generate Access Token ---
    public String generateAccessToken(UUID uuid, String username, Collection<? extends GrantedAuthority> authorities, boolean subscribed) {
        return generateToken(uuid, username, authorities, subscribed, accessTokenExpirationMs);
    }

    // --- Generate Refresh Token ---
    public String generateRefreshToken(UUID uuid, String username, Collection<? extends GrantedAuthority> authorities, boolean subscribed) {
        return generateToken(uuid, username, authorities, subscribed, refreshTokenExpirationMs);
    }

    // --- Core token generator (now includes uuid) ---
    private String generateToken(UUID uuid, String username, Collection<? extends GrantedAuthority> authorities, boolean subscribed, long expirationMs) {
        Set<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)                   // email as subject
                .claim("uuid", uuid.toString())         // ✅ add uuid claim
                .claim("roles", roles)
                .claim("Subscribed", subscribed)
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
