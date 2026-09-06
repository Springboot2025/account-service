package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.LetterOfAdviceDocumentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentSaveRequest;
import com.legalpro.accountservice.dto.SendLetterOfAdviceRequest;
import com.legalpro.accountservice.dto.SignatureRequest;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.LetterOfAdviceDocument;
import com.legalpro.accountservice.enums.LetterOfAdviceStatus;
import com.legalpro.accountservice.mapper.LetterOfAdviceDocumentMapper;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.LetterOfAdviceDocumentRepository;
import com.legalpro.accountservice.service.DeviceTokenService;
import com.legalpro.accountservice.service.EmailService;
import com.legalpro.accountservice.service.LetterOfAdviceDocumentService;
import com.legalpro.accountservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LetterOfAdviceDocumentServiceImpl implements LetterOfAdviceDocumentService {

    private final LetterOfAdviceDocumentRepository documentRepository;
    private final LetterOfAdviceDocumentMapper documentMapper;
    private final LegalCaseRepository legalCaseRepository;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final DeviceTokenService deviceTokenService;
    private final NotificationService notificationService;

    @Override
    public Optional<LetterOfAdviceDocumentDto> getForLawyerByCase(UUID lawyerUuid, UUID caseUuid) {
        return documentRepository.findByCaseUuidAndDeletedAtIsNull(caseUuid)
                .filter(doc -> doc.getLawyerUuid().equals(lawyerUuid))
                .map(documentMapper::toDto);
    }

    @Override
    public LetterOfAdviceDocumentDto getForClient(UUID clientUuid, UUID documentUuid) {
        LetterOfAdviceDocument entity = documentRepository
                .findByUuidAndClientUuidAndDeletedAtIsNull(documentUuid, clientUuid)
                .orElseThrow(() -> new RuntimeException("Letter of Advice not found"));

        if (entity.getStatus() == LetterOfAdviceStatus.DRAFT || entity.getStatus() == LetterOfAdviceStatus.LAWYER_SIGNED) {
            throw new RuntimeException("Letter of Advice not found");
        }

        return documentMapper.toDto(entity);
    }

    @Override
    @Transactional
    public LetterOfAdviceDocumentDto saveDraft(UUID lawyerUuid, UUID caseUuid, LetterOfAdviceDocumentSaveRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Letter content is required");
        }

        LegalCase legalCase = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new RuntimeException("Case not found"));
        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new RuntimeException("Access denied: not your case");
        }

        LetterOfAdviceDocument entity = documentRepository.findByCaseUuidAndDeletedAtIsNull(caseUuid)
                .orElse(null);

        if (entity == null) {
            entity = LetterOfAdviceDocument.builder()
                    .lawyerUuid(lawyerUuid)
                    .clientUuid(legalCase.getClientUuid())
                    .caseUuid(caseUuid)
                    .status(LetterOfAdviceStatus.DRAFT)
                    .build();
        } else if (entity.getStatus() != LetterOfAdviceStatus.DRAFT) {
            throw new IllegalStateException(
                    "This Letter of Advice has already been signed and can no longer be edited as a draft");
        }

        String caseTitle = legalCase.getListing() != null ? legalCase.getListing() : legalCase.getName();
        entity.setTemplateUuid(request.getTemplateUuid());
        entity.setTitle(request.getTitle() != null ? request.getTitle() : caseTitle);
        entity.setContent(request.getContent());

        return documentMapper.toDto(documentRepository.save(entity));
    }

    @Override
    @Transactional
    public LetterOfAdviceDocumentDto lawyerSign(UUID lawyerUuid, UUID documentUuid, SignatureRequest request) {
        if (request.getSignatureDataUrl() == null || request.getSignatureDataUrl().isBlank()) {
            throw new IllegalArgumentException("A signature is required");
        }

        LetterOfAdviceDocument entity = findOwnedByLawyer(lawyerUuid, documentUuid);
        if (entity.getStatus() != LetterOfAdviceStatus.DRAFT) {
            throw new IllegalStateException("This Letter of Advice has already been signed");
        }

        if (request.getContent() != null && !request.getContent().isBlank()) {
            entity.setContent(request.getContent());
        }
        entity.setLawyerSignature(request.getSignatureDataUrl());
        entity.setLawyerSignedAt(LocalDateTime.now());
        entity.setStatus(LetterOfAdviceStatus.LAWYER_SIGNED);

        return documentMapper.toDto(documentRepository.save(entity));
    }

    @Override
    @Transactional
    public LetterOfAdviceDocumentDto sendToClient(UUID lawyerUuid, UUID documentUuid, SendLetterOfAdviceRequest request) {
        LetterOfAdviceDocument entity = findOwnedByLawyer(lawyerUuid, documentUuid);
        if (entity.getStatus() != LetterOfAdviceStatus.LAWYER_SIGNED) {
            throw new IllegalStateException("You must sign this Letter of Advice before sending it to the client");
        }

        entity.setStatus(LetterOfAdviceStatus.SENT_TO_CLIENT);
        entity.setSentToClientAt(LocalDateTime.now());
        LetterOfAdviceDocument saved = documentRepository.save(entity);

        notifyUser(
                saved.getClientUuid(),
                "New Letter of Advice",
                "Your lawyer has sent you a Letter of Advice — please review and sign it.",
                saved.getUuid()
        );

        // Email is a purely optional courtesy copy — the real delivery is the
        // in-app notification + Letters inbox entry above, which have already
        // succeeded by this point. Only attempt email if the lawyer explicitly
        // typed one in, and never let an email provider failure (e.g. a
        // SendGrid quota error) turn this into a failed request.
        String recipientEmail = request != null ? request.getRecipientEmail() : null;
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            try {
                String subject = (request.getSubject() != null && !request.getSubject().isBlank())
                        ? request.getSubject()
                        : "Letter of Advice - " + saved.getTitle();
                String note = request.getMessage() != null ? request.getMessage() : "";
                String frontendBaseUrl = System.getenv("FRONTEND_BASE_URL") != null
                        ? System.getenv("FRONTEND_BASE_URL")
                        : "https://bossjustice.com.au";
                String signingLink = frontendBaseUrl + "/dashboard/communications/letter-of-advice/" + saved.getUuid();
                String body = "<p>" + note + "</p>"
                        + "<p><a href=\"" + signingLink + "\">Click here to view and sign your Letter of Advice</a></p>"
                        + "<hr/>" + saved.getContent();
                emailService.sendEmail(recipientEmail, subject, body);
            } catch (Exception ex) {
                log.error("Optional email copy of Letter of Advice {} failed to send to {}: {}",
                        saved.getUuid(), recipientEmail, ex.getMessage(), ex);
            }
        }

        return documentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public LetterOfAdviceDocumentDto clientSign(UUID clientUuid, UUID documentUuid, SignatureRequest request) {
        if (request.getSignatureDataUrl() == null || request.getSignatureDataUrl().isBlank()) {
            throw new IllegalArgumentException("A signature is required");
        }

        LetterOfAdviceDocument entity = documentRepository
                .findByUuidAndClientUuidAndDeletedAtIsNull(documentUuid, clientUuid)
                .orElseThrow(() -> new RuntimeException("Letter of Advice not found"));

        if (entity.getStatus() != LetterOfAdviceStatus.SENT_TO_CLIENT) {
            throw new IllegalStateException("This Letter of Advice is not yet ready for your signature");
        }

        entity.setClientSignature(request.getSignatureDataUrl());
        entity.setClientSignedAt(LocalDateTime.now());
        entity.setStatus(LetterOfAdviceStatus.CLIENT_SIGNED);

        LetterOfAdviceDocument saved = documentRepository.save(entity);

        String clientName = accountRepository.findByUuid(clientUuid)
                .map(this::extractName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Your client");

        notifyUser(
                saved.getLawyerUuid(),
                "Letter of Advice Signed",
                clientName + " has signed the Letter of Advice \"" + saved.getTitle() + "\".",
                saved.getUuid()
        );

        return documentMapper.toDto(saved);
    }

    private String extractName(Account account) {
        if (account == null || account.getPersonalDetails() == null) return null;
        var personalDetails = account.getPersonalDetails();
        String firstName = personalDetails.has("firstName") ? personalDetails.get("firstName").asText() : "";
        String lastName = personalDetails.has("lastName") ? personalDetails.get("lastName").asText() : "";
        return (firstName + " " + lastName).trim();
    }

    private void notifyUser(UUID userUuid, String title, String body, UUID documentUuid) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "LETTER_OF_ADVICE");
            data.put("documentUuid", documentUuid.toString());

            deviceTokenService.getTokensForUser(userUuid)
                    .forEach(token -> notificationService.sendNotificationWithData(
                            userUuid, token.getFcmToken(), title, body, data));
        } catch (Exception ex) {
            log.error("Failed to send Letter of Advice notification to user {}: {}", userUuid, ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void deleteForLawyer(UUID lawyerUuid, UUID documentUuid) {
        LetterOfAdviceDocument entity = findOwnedByLawyer(lawyerUuid, documentUuid);
        entity.setDeletedAt(LocalDateTime.now());
        documentRepository.save(entity);
    }

    private LetterOfAdviceDocument findOwnedByLawyer(UUID lawyerUuid, UUID documentUuid) {
        return documentRepository.findByUuidAndLawyerUuidAndDeletedAtIsNull(documentUuid, lawyerUuid)
                .orElseThrow(() -> new RuntimeException("Letter of Advice not found"));
    }
}
