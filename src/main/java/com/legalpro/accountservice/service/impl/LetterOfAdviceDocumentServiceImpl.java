package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.LetterOfAdviceChangeRequestCreateRequest;
import com.legalpro.accountservice.dto.LetterOfAdviceChangeRequestDto;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceHistoryItemDto;
import com.legalpro.accountservice.dto.LetterOfAdviceDocumentSaveRequest;
import com.legalpro.accountservice.dto.SendLetterOfAdviceRequest;
import com.legalpro.accountservice.dto.SignatureRequest;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.LetterOfAdviceChangeRequest;
import com.legalpro.accountservice.entity.LetterOfAdviceDocument;
import com.legalpro.accountservice.enums.LetterOfAdviceChangeCategory;
import com.legalpro.accountservice.enums.LetterOfAdviceStatus;
import com.legalpro.accountservice.mapper.LetterOfAdviceDocumentMapper;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.LetterOfAdviceChangeRequestRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LetterOfAdviceDocumentServiceImpl implements LetterOfAdviceDocumentService {

    private final LetterOfAdviceDocumentRepository documentRepository;
    private final LetterOfAdviceChangeRequestRepository changeRequestRepository;
    private final LetterOfAdviceDocumentMapper documentMapper;
    private final LegalCaseRepository legalCaseRepository;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final DeviceTokenService deviceTokenService;
    private final NotificationService notificationService;

    @Override
    public Optional<LetterOfAdviceDocumentDto> getForLawyerByCase(UUID lawyerUuid, UUID caseUuid) {
        return documentRepository.findByCaseUuidAndDeletedAtIsNullAndSupersededAtIsNull(caseUuid)
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

        LetterOfAdviceDocument entity = documentRepository.findByCaseUuidAndDeletedAtIsNullAndSupersededAtIsNull(caseUuid)
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

        if (entity.getSupersededAt() != null) {
            throw new IllegalStateException("This Letter of Advice has been replaced by a newer version");
        }
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


    private static final int MAX_CHANGE_REQUEST_MESSAGE_LENGTH = 2000;

    @Override
    @Transactional
    public LetterOfAdviceChangeRequestDto createChangeRequest(
            UUID clientUuid, UUID documentUuid, LetterOfAdviceChangeRequestCreateRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Please describe the changes you would like");
        }
        if (message.length() > MAX_CHANGE_REQUEST_MESSAGE_LENGTH) {
            throw new IllegalArgumentException(
                    "Your message is too long (maximum " + MAX_CHANGE_REQUEST_MESSAGE_LENGTH + " characters)");
        }

        LetterOfAdviceChangeCategory category;
        try {
            category = LetterOfAdviceChangeCategory.valueOf(
                    request.getCategory() == null ? "" : request.getCategory().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Please choose what the issue is about");
        }

        LetterOfAdviceDocument document = documentRepository
                .findByUuidAndClientUuidAndDeletedAtIsNull(documentUuid, clientUuid)
                .orElseThrow(() -> new RuntimeException("Letter of Advice not found"));

        if (document.getSupersededAt() != null) {
            throw new IllegalStateException("This Letter of Advice has been replaced by a newer version");
        }
        if (document.getStatus() != LetterOfAdviceStatus.SENT_TO_CLIENT) {
            throw new IllegalStateException("Changes can only be requested before you have signed this letter");
        }

        LetterOfAdviceChangeRequest saved = changeRequestRepository.save(
                LetterOfAdviceChangeRequest.builder()
                        .documentUuid(documentUuid)
                        .clientUuid(clientUuid)
                        .category(category)
                        .message(message)
                        .build());

        String clientName = accountRepository.findByUuid(clientUuid)
                .map(this::extractName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Your client");

        notifyUser(
                document.getLawyerUuid(),
                "Changes requested on Letter of Advice",
                clientName + " has requested changes to \"" + document.getTitle() + "\".",
                document.getUuid(),
                "LETTER_OF_ADVICE_CHANGE_REQUEST"
        );

        return toChangeRequestDto(saved);
    }

    @Override
    public List<LetterOfAdviceChangeRequestDto> getChangeRequestsForLawyer(UUID lawyerUuid, UUID documentUuid) {
        findOwnedByLawyer(lawyerUuid, documentUuid);
        return changeRequestRepository.findAllByDocumentUuidOrderByCreatedAtDesc(documentUuid)
                .stream()
                .map(this::toChangeRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public LetterOfAdviceChangeRequestDto markChangeRequestRead(UUID lawyerUuid, UUID documentUuid, UUID requestUuid) {
        findOwnedByLawyer(lawyerUuid, documentUuid);
        LetterOfAdviceChangeRequest changeRequest = changeRequestRepository
                .findByUuidAndDocumentUuid(requestUuid, documentUuid)
                .orElseThrow(() -> new RuntimeException("Change request not found"));
        if (changeRequest.getReadAt() == null) {
            changeRequest.setReadAt(LocalDateTime.now());
            changeRequest = changeRequestRepository.save(changeRequest);
        }
        return toChangeRequestDto(changeRequest);
    }

    @Override
    @Transactional
    public void markAllChangeRequestsRead(UUID lawyerUuid, UUID documentUuid) {
        findOwnedByLawyer(lawyerUuid, documentUuid);
        LocalDateTime now = LocalDateTime.now();
        List<LetterOfAdviceChangeRequest> unread =
                changeRequestRepository.findAllByDocumentUuidAndReadAtIsNull(documentUuid);
        unread.forEach(item -> item.setReadAt(now));
        changeRequestRepository.saveAll(unread);
    }

    private LetterOfAdviceChangeRequestDto toChangeRequestDto(LetterOfAdviceChangeRequest entity) {
        return LetterOfAdviceChangeRequestDto.builder()
                .uuid(entity.getUuid())
                .documentUuid(entity.getDocumentUuid())
                .category(entity.getCategory())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }

    private String extractName(Account account) {
        if (account == null || account.getPersonalDetails() == null) return null;
        var personalDetails = account.getPersonalDetails();
        String firstName = personalDetails.has("firstName") ? personalDetails.get("firstName").asText() : "";
        String lastName = personalDetails.has("lastName") ? personalDetails.get("lastName").asText() : "";
        return (firstName + " " + lastName).trim();
    }

    private void notifyUser(UUID userUuid, String title, String body, UUID documentUuid) {
        notifyUser(userUuid, title, body, documentUuid, "LETTER_OF_ADVICE");
    }

    private void notifyUser(UUID userUuid, String title, String body, UUID documentUuid, String type) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", type);
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


    @Override
    @Transactional
    public LetterOfAdviceDocumentDto supersede(UUID lawyerUuid, UUID documentUuid) {
        LetterOfAdviceDocument entity = findOwnedByLawyer(lawyerUuid, documentUuid);
        if (entity.getSupersededAt() != null) {
            throw new IllegalStateException("This Letter of Advice has already been replaced");
        }
        if (entity.getStatus() == LetterOfAdviceStatus.DRAFT) {
            throw new IllegalStateException("A draft doesn't need replacing -- edit it directly");
        }

        entity.setSupersededAt(LocalDateTime.now());
        LetterOfAdviceDocument saved = documentRepository.save(entity);

        // Only worth telling the client if they were waiting on this one.
        if (saved.getStatus() == LetterOfAdviceStatus.SENT_TO_CLIENT) {
            notifyUser(
                    saved.getClientUuid(),
                    "Letter of Advice replaced",
                    "Your lawyer is preparing an updated version of \"" + saved.getTitle()
                            + "\". You don't need to sign this one.",
                    saved.getUuid()
            );
        }

        return documentMapper.toDto(saved);
    }

    @Override
    public List<LetterOfAdviceHistoryItemDto> getHistoryForLawyer(UUID lawyerUuid, UUID caseUuid) {
        List<LetterOfAdviceDocument> documents =
                documentRepository.findAllByCaseUuidAndDeletedAtIsNullOrderByCreatedAtAsc(caseUuid).stream()
                        .filter(doc -> doc.getLawyerUuid().equals(lawyerUuid))
                        .toList();

        List<LetterOfAdviceHistoryItemDto> history = new java.util.ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            LetterOfAdviceDocument doc = documents.get(i);
            history.add(LetterOfAdviceHistoryItemDto.builder()
                    .uuid(doc.getUuid())
                    .version(i + 1)
                    .title(doc.getTitle())
                    .status(doc.getStatus())
                    .current(doc.getSupersededAt() == null)
                    .createdAt(doc.getCreatedAt())
                    .lawyerSignedAt(doc.getLawyerSignedAt())
                    .sentToClientAt(doc.getSentToClientAt())
                    .clientSignedAt(doc.getClientSignedAt())
                    .supersededAt(doc.getSupersededAt())
                    .changeRequestCount(changeRequestRepository.countByDocumentUuid(doc.getUuid()))
                    .unreadChangeRequestCount(
                            changeRequestRepository.countByDocumentUuidAndReadAtIsNull(doc.getUuid()))
                    .build());
        }
        java.util.Collections.reverse(history); // newest first
        return history;
    }

    private LetterOfAdviceDocument findOwnedByLawyer(UUID lawyerUuid, UUID documentUuid) {
        return documentRepository.findByUuidAndLawyerUuidAndDeletedAtIsNull(documentUuid, lawyerUuid)
                .orElseThrow(() -> new RuntimeException("Letter of Advice not found"));
    }
}
