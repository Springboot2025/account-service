package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ActivityLogDto;
import com.legalpro.accountservice.entity.ActivityLog;
import com.legalpro.accountservice.mapper.ActivityLogMapper;
import com.legalpro.accountservice.repository.ActivityLogRepository;
import com.legalpro.accountservice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ActivityLogRepository activityLogRepository;

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
}
