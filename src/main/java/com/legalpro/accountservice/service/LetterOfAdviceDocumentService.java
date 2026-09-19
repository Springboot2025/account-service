package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LetterOfAdviceChangeRequestCreateRequest;
import com.legalpro.accountservice.dto.LetterOfAdviceChangeRequestDto;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceHistoryItemDto;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentSaveRequest;
import com.legalpro.accountservice.dto.SendLetterOfAdviceRequest;
import com.legalpro.accountservice.dto.SignatureRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LetterOfAdviceDocumentService {

    Optional<LetterOfAdviceDocumentDto> getForLawyerByCase(UUID lawyerUuid, UUID caseUuid);

    LetterOfAdviceDocumentDto getForClient(UUID clientUuid, UUID documentUuid);

    LetterOfAdviceDocumentDto saveDraft(UUID lawyerUuid, UUID caseUuid, LetterOfAdviceDocumentSaveRequest request);

    LetterOfAdviceDocumentDto lawyerSign(UUID lawyerUuid, UUID documentUuid, SignatureRequest request);

    LetterOfAdviceDocumentDto sendToClient(UUID lawyerUuid, UUID documentUuid, SendLetterOfAdviceRequest request);

    LetterOfAdviceDocumentDto clientSign(UUID clientUuid, UUID documentUuid, SignatureRequest request);

    void deleteForLawyer(UUID lawyerUuid, UUID documentUuid);

    /** Marks a locked letter as replaced so a new one can be started for the case. */
    LetterOfAdviceDocumentDto supersede(UUID lawyerUuid, UUID documentUuid);

    List<LetterOfAdviceHistoryItemDto> getHistoryForLawyer(UUID lawyerUuid, UUID caseUuid);

    LetterOfAdviceChangeRequestDto createChangeRequest(
            UUID clientUuid, UUID documentUuid, LetterOfAdviceChangeRequestCreateRequest request);

    List<LetterOfAdviceChangeRequestDto> getChangeRequestsForLawyer(UUID lawyerUuid, UUID documentUuid);

    LetterOfAdviceChangeRequestDto markChangeRequestRead(UUID lawyerUuid, UUID documentUuid, UUID requestUuid);

    void markAllChangeRequestsRead(UUID lawyerUuid, UUID documentUuid);
}
