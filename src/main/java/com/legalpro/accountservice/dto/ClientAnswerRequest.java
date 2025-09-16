package com.legalpro.accountservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.entity.QuestionType;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class ClientAnswerRequest {
    private UUID clientUuid;
    private QuestionType questionType;
    private Map<String, Object> answers;
}

