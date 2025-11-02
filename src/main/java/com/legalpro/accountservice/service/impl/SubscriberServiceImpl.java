package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.SubscriberDto;
import com.legalpro.accountservice.entity.Subscriber;
import com.legalpro.accountservice.mapper.SubscriberMapper;
import com.legalpro.accountservice.repository.SubscriberRepository;
import com.legalpro.accountservice.service.SubscriberService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriberServiceImpl implements SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final SubscriberMapper subscriberMapper;

    @Override
    public SubscriberDto addSubscriber(String email) {
        log.info("Adding new subscriber: {}", email);

        // Prevent duplicates
        subscriberRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.getIsActive()) {
                throw new RuntimeException("This email is already subscribed.");
            } else {
                existing.setIsActive(true);
                subscriberRepository.save(existing);
                throw new RuntimeException("Subscription reactivated for existing user.");
            }
        });

        Subscriber subscriber = Subscriber.builder()
                .email(email.toLowerCase())
                .build();

        Subscriber saved = subscriberRepository.save(subscriber);
        return subscriberMapper.toDto(saved);
    }

    @Override
    public List<SubscriberDto> getAllSubscribers() {
        log.info("Fetching all subscribers...");
        return subscriberRepository.findAll()
                .stream()
                .map(subscriberMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivateSubscriber(UUID subscriberUuid) {
        Subscriber subscriber = subscriberRepository.findByUuid(subscriberUuid)
                .orElseThrow(() -> new RuntimeException("Subscriber not found"));
        subscriber.setIsActive(false);
        subscriberRepository.save(subscriber);
    }

    @Override
    public void sendNotificationToSubscribers(List<UUID> subscriberUuids, String subject, String messageBody) {
        List<Subscriber> subscribers = subscriberRepository.findAll()
                .stream()
                .filter(s -> subscriberUuids == null || subscriberUuids.contains(s.getUuid()))
                .collect(Collectors.toList());

        for (Subscriber s : subscribers) {
            log.info("Sending notification to {}", s.getEmail());
            //emailService.sendEmail(s.getEmail(), subject, messageBody);
        }
    }
}
