package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.QuoteSummaryDto;
import java.util.UUID;

public interface QuoteSummaryService {
    QuoteSummaryDto getSummary(UUID lawyerUuid);
}
