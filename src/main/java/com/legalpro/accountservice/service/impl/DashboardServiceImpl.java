package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ActivityLogDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.ActivityLog;
import com.legalpro.accountservice.mapper.ActivityLogMapper;
import com.legalpro.accountservice.repository.ActivityLogRepository;
import com.legalpro.accountservice.service.DashboardService;
import com.legalpro.accountservice.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ActivityLogRepository activityLogRepository;
    private final ProfileService profileService;

    @Override
    public List<ActivityLogDto> getRecentActivities(UUID lawyerUuid, Integer limit) {

        int size = (limit != null && limit > 0) ? limit : 10;

        Pageable pageable = PageRequest.of(0, size);

        List<ActivityLog> logs =
                activityLogRepository.findByLawyerUuidOrderByTimestampDesc(
                        lawyerUuid,
                        pageable
                );

        return logs.stream()
                .map(ActivityLogMapper::toDto)
                .toList();
    }

    @Override
    public List<ActivityLogDto> getClientRecentActivities(UUID clientUuid, Integer limit) {

        int size = (limit != null && limit > 0) ? limit : 10;
        Pageable pageable = PageRequest.of(0, size);

        List<ActivityLog> logs = activityLogRepository
                .findByClientUuidOrderByTimestampDesc(clientUuid, pageable);

        if (logs.isEmpty()) return List.of();

        // 1️⃣ Collect all unique UUIDs (actor + client + lawyer)
        Set<UUID> uuids = new HashSet<>();
        logs.forEach(log -> {
            if (log.getClientUuid() != null) uuids.add(log.getClientUuid());
            if (log.getLawyerUuid() != null) uuids.add(log.getLawyerUuid());
            // actorUuid is same as actorName used earlier
            // actorUuid = whichever user performed action
            if (log.getActorUuid() != null) uuids.add(log.getActorUuid());
        });

        // 2️⃣ Load all accounts in one DB call
        Map<UUID, Account> accounts = profileService.loadAccounts(uuids);

        // 3️⃣ Map logs + inject profile pictures
        return logs.stream().map(log -> {
            ActivityLogDto dto = ActivityLogMapper.toDto(log);

            // Actor
            Account actorAcc = accounts.get(log.getActorUuid());
            dto.setActorProfilePictureUrl(
                    profileService.getProfilePicture(actorAcc)
            );

            // Client
            Account clientAcc = accounts.get(log.getClientUuid());
            dto.setClientProfilePictureUrl(
                    profileService.getProfilePicture(clientAcc)
            );

            // Lawyer
            Account lawyerAcc = accounts.get(log.getLawyerUuid());
            dto.setLawyerProfilePictureUrl(
                    profileService.getProfilePicture(lawyerAcc)
            );

            return dto;

        }).toList();
    }
}
