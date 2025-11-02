package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.SubscriberDto;
import java.util.List;
import java.util.UUID;

public interface SubscriberService {

    // --- Public ---
    SubscriberDto addSubscriber(String email);

    // --- Admin only ---
    List<SubscriberDto> getAllSubscribers();

    void deactivateSubscriber(UUID subscriberUuid);

    void sendNotificationToSubscribers(List<UUID> subscriberUuids, String subject, String messageBody);
}
