package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ClientLetterDto;
import com.legalpro.accountservice.dto.ClientLetterView;
import com.legalpro.accountservice.repository.SharedDocumentRepository;
import com.legalpro.accountservice.service.ClientLetterService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClientLetterServiceImpl implements ClientLetterService {

    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com";

    private final SharedDocumentRepository repository;

    public ClientLetterServiceImpl(SharedDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ClientLetterDto> getClientLetters(UUID clientUuid) {

        return repository.findLettersForClient(clientUuid)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
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
}
