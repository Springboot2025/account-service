package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.entity.DeviceToken;
import com.legalpro.accountservice.repository.DeviceTokenRepository;
import com.legalpro.accountservice.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public DeviceToken registerOrUpdate(UUID userUuid, String deviceId, String fcmToken, String platform) {
        return deviceTokenRepository.findByUserUuid(userUuid)
                .map(existing -> {
                    existing.setDeviceId(deviceId);
                    existing.setFcmToken(fcmToken);
                    existing.setPlatform(platform);
                    return deviceTokenRepository.save(existing);
                })
                .orElseGet(() -> deviceTokenRepository.save(DeviceToken.builder()
                        .userUuid(userUuid)
                        .deviceId(deviceId)
                        .fcmToken(fcmToken)
                        .platform(platform)
                        .build()));
    }

    @Override
    public List<DeviceToken> getTokensForUser(UUID userUuid) {
        return deviceTokenRepository.findAllByUserUuid(userUuid);
    }

    @Override
    public void deleteByDeviceId(String deviceId) {
        deviceTokenRepository.findByDeviceId(deviceId)
                .ifPresent(deviceTokenRepository::delete);
    }
}
