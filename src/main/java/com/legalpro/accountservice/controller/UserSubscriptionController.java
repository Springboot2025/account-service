package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.CreateUserSubscriptionDto;
import com.legalpro.accountservice.dto.UpdateUserSubscriptionDto;
import com.legalpro.accountservice.dto.UserSubscriptionDto;
import com.legalpro.accountservice.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user-subscriptions")
@RequiredArgsConstructor
public class UserSubscriptionController {

    private final UserSubscriptionService userSubscriptionService;

    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<UserSubscriptionDto>> createUserSubscription(
            @RequestBody CreateUserSubscriptionDto dto
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "User subscription created successfully",
                        userSubscriptionService.createUserSubscription(dto)
                )
        );
    }

    // UPDATE
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<UserSubscriptionDto>> updateUserSubscription(
            @PathVariable UUID uuid,
            @RequestBody UpdateUserSubscriptionDto dto
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "User subscription updated successfully",
                        userSubscriptionService.updateUserSubscription(uuid, dto)
                )
        );
    }
}