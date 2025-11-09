package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.DeviceToken;

import java.util.List;
import java.util.UUID;

public interface DeviceTokenService {

    /**
     * Register or update a device's FCM token for a user.
     */
    DeviceToken registerOrUpdate(UUID userUuid, String deviceId, String fcmToken, String platform);

    /**
     * Fetch all registered device tokens for a given user.
     */
    List<DeviceToken> getTokensForUser(UUID userUuid);

    /**
     * Delete a specific device registration.
     */
    void deleteByDeviceId(String deviceId);

    void removeAllTokens(UUID userUuid);
}
