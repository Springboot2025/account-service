package com.legalpro.accountservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClientLetterView {

    UUID getSharedUuid();

    UUID getCaseUuid();
    String getCaseNumber();
    String getCaseTitle();

    UUID getDocumentUuid();
    String getDocumentName();
    String getFileType();
    String getFileUrl();

    LocalDateTime getSentDate();
    String getRemarks();
}
