package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.DisputeDto;

import java.util.List;
import java.util.UUID;

public interface DisputeService {

    // User submits dispute
    DisputeDto submitDispute(DisputeDto dto);

    // Admin views all disputes
    List<DisputeDto> getAll();

    // Admin views single dispute + documents (used later)
    DisputeDto getOne(UUID disputeUuid);

    List<DisputeDto> getAllWithDocumentCount();

}
