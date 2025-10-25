package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.dto.ContactSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContactRepositoryCustom {
    Page<ContactSummaryDto> findContactsForLawyer(UUID lawyerUuid, String search, String filter, Pageable pageable);
}
