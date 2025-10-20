package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.entity.DeviceToken;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    // --- Register or update ---
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DeviceToken>> registerDevice(
            @RequestParam String deviceId,
            @RequestParam String fcmToken,
            @RequestParam String platform,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userUuid = userDetails.getUuid();
        DeviceToken token = deviceTokenService.registerOrUpdate(userUuid, deviceId, fcmToken, platform);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Device registered or updated successfully",
                token
        ));
    }

    // --- Delete device ---
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<String>> deleteDevice(
            @PathVariable String deviceId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        deviceTokenService.deleteByDeviceId(deviceId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Device unregistered successfully",
                null
        ));
    }

    // --- ✅ Get devices for a specific user ---
    @GetMapping("/{userUuid}")
    public ResponseEntity<ApiResponse<List<DeviceToken>>> getDevicesForUser(
            @PathVariable UUID userUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // (Optional) Add check if only self or admin can view
        List<DeviceToken> tokens = deviceTokenService.getTokensForUser(userUuid);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                "Devices fetched successfully",
                tokens
        ));
    }
}
