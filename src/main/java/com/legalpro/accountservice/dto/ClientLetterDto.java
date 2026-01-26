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

