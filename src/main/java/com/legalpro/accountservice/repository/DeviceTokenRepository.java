package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByDeviceId(String deviceId);
    List<DeviceToken> findAllByUserUuid(UUID userUuid);

    Optional<DeviceToken> findByUserUuid(UUID userUuid);

}
