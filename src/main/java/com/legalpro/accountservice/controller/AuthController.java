package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LoginRequest;
import com.legalpro.accountservice.dto.RegisterRequest;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.security.JwtUtil;
import com.legalpro.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

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
            @Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getAuthorities());

            ApiResponse<Map<String, String>> response =
                    ApiResponse.success(
                            HttpStatus.OK.value(),
                            "Login successful",
                            Map.of("token", token)
                    );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            ApiResponse<Map<String, String>> response =
                    ApiResponse.error(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Invalid email or password"
                    );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (AuthenticationException e) {
            ApiResponse<Map<String, String>> response =
                    ApiResponse.error(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Authentication failed"
                    );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
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

}
