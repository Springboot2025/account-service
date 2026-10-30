package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.TrialReminderSend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrialReminderSendRepository extends JpaRepository<TrialReminderSend, Long> {
    boolean existsByUserSubscriptionUuidAndDaysBefore(UUID userSubscriptionUuid, Integer daysBefore);
}
