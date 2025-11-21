package com.legalpro.accountservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtSupportMaterialDto {

    private Long id;
    private UUID clientUuid;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Map<String, Object> description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
