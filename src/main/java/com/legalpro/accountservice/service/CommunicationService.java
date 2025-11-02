package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ClientCommunicationSummaryDto;

import java.util.List;
import java.util.UUID;

public interface CommunicationService {

    /**
     * Fetches a summary of all client communications for a given lawyer.
     * This includes last message, last quote, and last appointment per client.
     *
     * @param lawyerUuid the lawyer’s UUID
     * @param search optional name or email search filter
     * @return list of summaries per client
     */
    List<ClientCommunicationSummaryDto> getLawyerCommunications(UUID lawyerUuid, String search);
}
