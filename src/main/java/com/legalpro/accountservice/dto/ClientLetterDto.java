package com.legalpro.accountservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientLetterDto {

    private UUID sharedUuid;
    private CaseInfo caseInfo;
    private DocumentInfo document;
    private LawyerInfo lawyer;
    private LocalDateTime sentDate;
    private String remarks;

    /** "SHARED_DOCUMENT" (default, existing behaviour) or "LETTER_OF_ADVICE". */
    @Builder.Default
    private String type = "SHARED_DOCUMENT";

    /** Only set when type is LETTER_OF_ADVICE — the id to view/sign it. */
    private UUID letterOfAdviceUuid;

    /** Only set when type is LETTER_OF_ADVICE — SENT_TO_CLIENT or CLIENT_SIGNED. */
    private String status;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CaseInfo {
        private UUID uuid;
        private String caseNumber;
        private String title;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentInfo {
        private UUID uuid;
        private String name;
        private String fileType;
        private String fileUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LawyerInfo {
        private UUID uuid;
        private String name;
        private String email;
        private String profilePictureUrl;
    }
}

