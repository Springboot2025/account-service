package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LoginRequest;
import com.legalpro.accountservice.dto.RegisterRequest;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.security.JwtUtil;
import com.legalpro.accountservice.service.AccountService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AccountService accountService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AccountService accountService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername(), userDetails.getAuthorities());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername(), userDetails.getAuthorities());

            // Send refresh token as HttpOnly cookie
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)   // set false for local dev without HTTPS
                    .path("/")
                    .sameSite("Strict")
                    .maxAge(30L * 24 * 60 * 60) // 30 days
                    .build();

            response.addHeader("Set-Cookie", refreshCookie.toString());

            // Send access token in body
            ApiResponse<Map<String, String>> apiResponse = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "Login successful",
                    Map.of("accessToken", accessToken)
            );

            return ResponseEntity.ok(apiResponse);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            Account account = accountService.register(request);

            ApiResponse<Map<String, String>> response =
                    ApiResponse.success(
                            HttpStatus.CREATED.value(),
                            "User registered successfully",
                            Map.of("email", account.getEmail())
                    );

            return ResponseEntity.status(HttpStatus.CREATED.value()).body(response);

        } catch (RuntimeException e) {
            ApiResponse<Map<String, String>> response =
                    ApiResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            e.getMessage()
                    );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(response);
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Invalid or missing refresh token"));
        }

        // Extract username + roles
        String username = jwtUtil.getUsernameFromJwt(refreshToken);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtUtil.getKey())   // <-- expose getKey() in JwtUtil
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) claims.get("roles");

        // Convert roles into GrantedAuthority
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(username, authorities);

        ApiResponse<Map<String, String>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Access token refreshed successfully",
                Map.of("accessToken", newAccessToken)
        );

        return ResponseEntity.ok(response);
    }



}
