package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.DocumentShareRecipientDto;

import java.util.List;
import java.util.UUID;

public interface DocumentShareRecipientService {
    List<DocumentShareRecipientDto> getRecipientsForLawyer(UUID lawyerUuid);
}
