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

    private final long accessTokenExpirationMs = 24 * 60 * 60 * 1000;             // 1 day
    private final long refreshTokenExpirationMs = 30L * 24 * 60 * 60 * 1000; // 30 days

    private final Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

    // --- Generate Access Token (short-lived) ---
    public String generateAccessToken(String username, Collection<? extends GrantedAuthority> authorities) {
        return generateToken(username, authorities, accessTokenExpirationMs);
    }

    // --- Generate Refresh Token (long-lived) ---
    public String generateRefreshToken(String username, Collection<? extends GrantedAuthority> authorities) {
        return generateToken(username, authorities, refreshTokenExpirationMs);
    }

    // --- Core token generator ---
    private String generateToken(String username, Collection<? extends GrantedAuthority> authorities, long expirationMs) {
        Set<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // --- Extract username ---
    public String getUsernameFromJwt(String token) {
        return parseClaims(token).getSubject();
    }

    // --- Extract roles ---
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> getRolesFromJwt(String token) {
        Claims claims = parseClaims(token);
        List<String> roles = claims.get("roles", List.class);
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
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

}
