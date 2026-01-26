package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ClientDashboardSummaryDto;

import java.util.UUID;

public interface ClientDashboardService {

    ClientDashboardSummaryDto getSummary(UUID clientUuid);

}
