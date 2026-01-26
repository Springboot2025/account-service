package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ClientLetterDto;
import com.legalpro.accountservice.dto.ClientLetterView;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.DocumentTemplateCenter;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.SharedDocument;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.SharedDocumentRepository;
import com.legalpro.accountservice.service.ClientLetterService;
import com.legalpro.accountservice.service.ProfileService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClientLetterServiceImpl implements ClientLetterService {

    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com";

    private final SharedDocumentRepository repository;
    private final ProfileService profileService;
    private final AccountRepository accountRepository;

    public ClientLetterServiceImpl(SharedDocumentRepository repository,
                                   ProfileService profileService,
                                   AccountRepository accountRepository) {
        this.repository = repository;
        this.profileService = profileService;
        this.accountRepository = accountRepository;
    }

    @Override
    public List<ClientLetterDto> getClientLetters(UUID clientUuid) {

        List<Object[]> rows = repository.findLettersForClient(clientUuid);

        return rows.stream().map(row -> {

            SharedDocument sd = (SharedDocument) row[0];
            LegalCase c = (LegalCase) row[1];
            DocumentTemplateCenter d = (DocumentTemplateCenter) row[2];

            Account lawyer = accountRepository.findByUuid(sd.getLawyerUuid())
                    .orElse(null);

            return ClientLetterDto.builder()
                    .sharedUuid(sd.getUuid())

                    .caseInfo(
                            ClientLetterDto.CaseInfo.builder()
                                    .uuid(c.getUuid())
                                    .caseNumber(c.getCaseNumber())
                                    .title(c.getListing() != null ? c.getListing() : c.getName())
                                    .build()
                    )

                    .document(
                            ClientLetterDto.DocumentInfo.builder()
                                    .uuid(d.getUuid())
                                    .name(d.getFileName())
                                    .fileType(d.getFileType())
                                    .fileUrl(d.getFileUrl())
                                    .build()
                    )

                    .lawyer(
                            ClientLetterDto.LawyerInfo.builder()
                                    .uuid(sd.getLawyerUuid())
                                    .name(extractName(lawyer))
                                    .email(lawyer != null ? lawyer.getEmail() : null)
                                    .profilePictureUrl(profileService.getProfilePicture(lawyer))
                                    .build()
                    )

                    .sentDate(sd.getCreatedAt())
                    .remarks(sd.getRemarks())
                    .build();

        }).toList();
    }

    private ClientLetterDto toDto(ClientLetterView v) {

        String publicUrl = v.getFileUrl();
        if (publicUrl != null && publicUrl.startsWith("gs://")) {
            publicUrl = GCS_PUBLIC_BASE + "/" + publicUrl.substring(5);
        }

        return ClientLetterDto.builder()
                .sharedUuid(v.getSharedUuid())
                .caseInfo(
                        ClientLetterDto.CaseInfo.builder()
                                .uuid(v.getCaseUuid())
                                .caseNumber(v.getCaseNumber())
                                .title(v.getCaseTitle())
                                .build()
                )
                .document(
                        ClientLetterDto.DocumentInfo.builder()
                                .uuid(v.getDocumentUuid())
                                .name(v.getDocumentName())
                                .fileType(v.getFileType())
                                .fileUrl(publicUrl)
                                .build()
                )
                .sentDate(v.getSentDate())
                .remarks(v.getRemarks())
                .build();
    }

    private String extractName(Account acc) {
        if (acc == null || acc.getPersonalDetails() == null) return null;

        var pd = acc.getPersonalDetails();
        String first = pd.has("firstName") ? pd.get("firstName").asText() : "";
        String last = pd.has("lastName") ? pd.get("lastName").asText() : "";

        return (first + " " + last).trim();
    }

}
