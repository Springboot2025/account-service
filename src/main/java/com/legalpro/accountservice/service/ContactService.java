package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ContactSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContactService {
    Page<ContactSummaryDto> getContactsForLawyer(UUID lawyerUuid, String search, String filter, Pageable pageable);
}
