package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.CreateUserSubscriptionDto;
import com.legalpro.accountservice.dto.UpdateUserSubscriptionDto;
import com.legalpro.accountservice.dto.UserSubscriptionDto;
import com.legalpro.accountservice.entity.UserSubscription;
import com.legalpro.accountservice.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    // CREATE
    public UserSubscriptionDto createUserSubscription(CreateUserSubscriptionDto dto) {

        UserSubscription entity = UserSubscription.builder()
                .userUuid(dto.getUserUuid())
                .userType(dto.getUserType())
                .planId(dto.getPlanId())
                .planDuration(dto.getPlanDuration())
                .startDate(dto.getStartDate())
                .renewsAt(dto.getRenewsAt())
                .status(0) // default INACTIVE
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserSubscription saved = userSubscriptionRepository.save(entity);

        return mapToDto(saved);
    }

    // UPDATE
    public UserSubscriptionDto updateUserSubscription(UUID uuid, UpdateUserSubscriptionDto dto) {

        UserSubscription entity = userSubscriptionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }

        if (dto.getPlanDuration() != null) {
            entity.setPlanDuration(dto.getPlanDuration());
        }

        if (dto.getRenewsAt() != null) {
            entity.setRenewsAt(dto.getRenewsAt());
        }

        entity.setUpdatedAt(LocalDateTime.now());

        UserSubscription saved = userSubscriptionRepository.save(entity);

        return mapToDto(saved);
    }

    // MAPPER
    private UserSubscriptionDto mapToDto(UserSubscription entity) {

        return UserSubscriptionDto.builder()
                .uuid(entity.getUuid())
                .userUuid(entity.getUserUuid())
                .userType(entity.getUserType())
                .planId(entity.getPlanId())
                .status(entity.getStatus())
                .planDuration(entity.getPlanDuration())
                .startDate(entity.getStartDate())
                .renewsAt(entity.getRenewsAt())
                .build();
    }
}