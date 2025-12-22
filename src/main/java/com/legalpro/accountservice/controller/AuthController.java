package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LoginRequest;
import com.legalpro.accountservice.dto.RegisterRequest;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.repository.CompanyRepository;
import com.legalpro.accountservice.repository.SubscriberRepository;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.security.JwtUtil;
import com.legalpro.accountservice.security.TokenBlacklistService;
import com.legalpro.accountservice.service.AccountService;
import com.legalpro.accountservice.service.DeviceTokenService;
import com.legalpro.accountservice.service.EmailService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.legalpro.accountservice.entity.Company;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AccountService accountService;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SubscriberRepository subscriberRepository;
    private final CompanyRepository companyRepository;
    private final DeviceTokenService deviceTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AccountService accountService,
                          EmailService emailService,
                          TokenBlacklistService tokenBlacklistService,
                          SubscriberRepository subscriberRepository,
                          CompanyRepository companyRepository,
                          DeviceTokenService deviceTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.accountService = accountService;
        this.emailService = emailService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.subscriberRepository = subscriberRepository;
        this.companyRepository = companyRepository;
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
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

            // Fetch UUID from Account
            Account account = accountService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isSubscribed = subscriberRepository.findByEmail(account.getEmail())
                    .map(sub -> Boolean.TRUE.equals(sub.getIsActive()))
                    .orElse(false);

            boolean isCompany = account.isCompany();
            boolean isCompanyMember = !account.isCompany() && account.getCompanyUuid() != null;

            String companyName = null;
            if (isCompany) {
                // ✅ company account → name from companies table
                companyName = companyRepository.findByUuid(account.getCompanyUuid())
                        .map(Company::getName)
                        .orElse(null);
            }
// else if member: leave companyName as null (as per your requirement)


            // ✅ Generate tokens with uuid included
            String accessToken = jwtUtil.generateAccessToken(
                    account.getUuid(),
                    userDetails.getUsername(),
                    userDetails.getAuthorities(),
                    isSubscribed,
                    isCompany,
                    isCompanyMember,
                    companyName
            );
            String refreshToken = jwtUtil.generateRefreshToken(
                    account.getUuid(),
                    userDetails.getUsername(),
                    userDetails.getAuthorities(),
                    isSubscribed,
                    isCompany,
                    isCompanyMember,
                    companyName
            );

            // ✅ Allow local dev without HTTPS
            boolean isLocalhost = "localhost".equalsIgnoreCase(request.getServerName());

            // Set refresh token as HttpOnly cookie
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(!isLocalhost)   // ✅ secure=false on localhost
                    .path("/")
                    .sameSite("None")       // ✅ must be None for cross-origin cookies
                    .maxAge(30L * 24 * 60 * 60) // 30 days
                    .build();

            response.addHeader("Set-Cookie", refreshCookie.toString());

            // Send access token, email, and uuid in body
            ApiResponse<Map<String, String>> apiResponse = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "Login successful",
                    Map.of(
                            "accessToken", accessToken,
                            "email", account.getEmail(),
                            "profilePictureUrl", Optional.ofNullable(account.getProfilePictureUrl()).orElse(""),
                            "uuid", account.getUuid().toString()
                    )
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
            // 1. Register user (without activating)
            Account account = accountService.register(request);

            // 5. Return response
            ApiResponse<Map<String, String>> response =
                    ApiResponse.success(
                            HttpStatus.CREATED.value(),
                            "User registered successfully. Verification email sent.",
                            Map.of("email", account.getEmail(), "uuid", account.getUuid().toString())
                    );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            ApiResponse<Map<String, String>> response =
                    ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }



    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Invalid or missing refresh token"));
        }

        // Extract claims from refresh token
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtUtil.getKey())
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        String username = claims.getSubject();
        String uuidStr = claims.get("uuid", String.class);  // ✅ extract uuid
        UUID uuid = UUID.fromString(uuidStr);

        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) claims.get("roles");

        // Convert roles into GrantedAuthority
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        boolean isSubscribed = Boolean.TRUE.equals(claims.get("Subscribed", Boolean.class));
        boolean isCompany = Boolean.TRUE.equals(claims.get("isCompany", Boolean.class));
        boolean isCompanyMember = Boolean.TRUE.equals(claims.get("isCompanyMember", Boolean.class));
        String companyName = claims.get("companyName", String.class);

        // ✅ Generate new tokens with uuid included
        String newAccessToken = jwtUtil.generateAccessToken(uuid, username, authorities, isSubscribed, isCompany, isCompanyMember, companyName);
        String newRefreshToken = jwtUtil.generateRefreshToken(uuid, username, authorities, isSubscribed, isCompany, isCompanyMember, companyName);

        // ✅ Allow local dev without HTTPS
        boolean isLocalhost = "localhost".equalsIgnoreCase(request.getServerName());

        // Replace old refresh token with new one
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(!isLocalhost)   // ✅ secure=false on localhost
                .path("/")
                .sameSite("None")       // ✅ must be None for cross-origin cookies
                .maxAge(7L * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        ApiResponse<Map<String, String>> apiResponse = ApiResponse.success(
                HttpStatus.OK.value(),
                "Access token refreshed successfully",
                Map.of("accessToken", newAccessToken)
        );

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyAccount(@RequestParam("token") UUID token) {
        Optional<Account> accountOpt = accountService.verifyAccount(token);

        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            return ResponseEntity.ok(
                    ApiResponse.success(
                            200,
                            "Account verified successfully",
                            Map.of(
                                    "uuid", account.getUuid().toString(),
                                    "email", account.getEmail()
                            )
                    )
            );
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Invalid or expired verification token"));
        }
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<String>> setPassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        String uuidStr = body.get("uuid");

        if (uuidStr == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "UUID and password are required"));
        }

        UUID uuid = UUID.fromString(uuidStr);

        try {
            accountService.setPassword(uuid, password);
            return ResponseEntity.ok(ApiResponse.success(200, "Password set successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Email is required"));
        }

        try {
            accountService.sendForgotPasswordEmail(email);
            return ResponseEntity.ok(ApiResponse.success(200, "Password reset email sent", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody Map<String, String> body) {
        String tokenStr = body.get("token");
        String newPassword = body.get("password");

        if (tokenStr == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Token and new password are required"));
        }

        try {
            accountService.resetPassword(UUID.fromString(tokenStr), newPassword);
            return ResponseEntity.ok(ApiResponse.success(200, "Password reset successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Clear refresh token cookie for client
        boolean isLocalhost = "localhost".equalsIgnoreCase(request.getServerName());
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(!isLocalhost)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", deleteCookie.toString());

        // Blacklist the access token if present so it becomes unusable immediately
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    Date expiry = jwtUtil.getExpiration(token);
                    tokenBlacklistService.blacklistToken(token, expiry);
                }
            } catch (Exception e) {
                // If token is invalid already, we just continue — cookie is still cleared
            }
        }

        deviceTokenService.removeAllTokens(userDetails.getUuid());

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Logout successful", null));
    }
}
