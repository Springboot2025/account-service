package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ActivityLogDto;

import java.util.List;
import java.util.UUID;

public interface DashboardService {
    List<ActivityLogDto> getRecentActivities(UUID lawyerUuid, Integer limit);
    List<ActivityLogDto> getClientRecentActivities(UUID clientUuid, Integer limit);

}
