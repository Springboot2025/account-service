package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LetterOfAdviceDocumentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentSaveRequest;
import com.legalpro.accountservice.dto.SendLetterOfAdviceRequest;
import com.legalpro.accountservice.dto.SignatureRequest;

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
}
