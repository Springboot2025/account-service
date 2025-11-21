package com.legalpro.accountservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.entity.QuestionType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAnswerDto {
    private Long id;
    private UUID clientUuid;
    private QuestionType questionType;
    private JsonNode answers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
