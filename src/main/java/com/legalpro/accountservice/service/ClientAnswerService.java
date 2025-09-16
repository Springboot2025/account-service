package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.entity.ClientAnswer;
import com.legalpro.accountservice.entity.QuestionType;
import com.legalpro.accountservice.repository.ClientAnswerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientAnswerService {

    private final ClientAnswerRepository repository;
    private final ObjectMapper objectMapper;

    public ClientAnswerService(ClientAnswerRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Save a new client answer.
     */
    public ClientAnswer save(ClientAnswer answer) {
        return repository.save(answer);
    }

    /**
     * Find a client answer by client UUID and question type.
     */
    public Optional<ClientAnswer> findByClientAndType(UUID clientUuid, QuestionType type) {
        return repository.findByClientUuidAndQuestionTypeAndDeletedAtIsNull(clientUuid, type);
    }

    /**
     * Find all active answers for a client.
     */
    public List<ClientAnswer> findAllByClient(UUID clientUuid) {
        return repository.findAllByClientUuidAndDeletedAtIsNull(clientUuid);
    }

    /**
     * Update existing client answers for a given UUID and type.
     */
    @Transactional
    public Optional<ClientAnswer> updateAnswers(UUID clientUuid, QuestionType type, Map<String, Object> answers) {
        return repository.findByClientUuidAndQuestionTypeAndDeletedAtIsNull(clientUuid, type)
                .map(existing -> {
                    JsonNode jsonAnswers = objectMapper.convertValue(answers, JsonNode.class);
                    existing.setAnswers(jsonAnswers);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existing);
                });
    }

    public JsonNode convertToJsonNode(Map<String, Object> answers) {
        return objectMapper.convertValue(answers, JsonNode.class);
    }

}
