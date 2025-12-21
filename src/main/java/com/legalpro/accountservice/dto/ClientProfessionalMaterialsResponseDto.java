package com.legalpro.accountservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientProfessionalMaterialsResponseDto {

    private Long categoryId;
    private String categoryName;
    private List<DocumentDto> documents;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentDto {

        private String uuid;
        private String fileName;
        private String fileType;
        private String fileUrl;   // ✅ PUBLIC URL (not gs://)
        private String followUp;
        private String description;
        private String createdAt;
    }
}
