package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserUuidOrderBySentAtDesc(UUID userUuid);
}
