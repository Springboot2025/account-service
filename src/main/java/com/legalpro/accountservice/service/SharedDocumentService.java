package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ShareDocumentRequestDto;
import com.legalpro.accountservice.dto.SharedDocumentResponseDto;

import java.util.List;
import java.util.UUID;

public interface SharedDocumentService {

    List<SharedDocumentResponseDto> shareDocument(
            UUID lawyerUuid,
            UUID documentUuid,
            ShareDocumentRequestDto request
    );
}
