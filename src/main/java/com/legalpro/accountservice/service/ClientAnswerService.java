package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.ClientAnswer;
import com.legalpro.accountservice.entity.QuestionType;
import com.legalpro.accountservice.repository.ClientAnswerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientAnswerService {

    private final ClientAnswerRepository repository;

    public ClientAnswerService(ClientAnswerRepository repository) {
        this.repository = repository;
    }

    public ClientAnswer save(ClientAnswer answer) {
        return repository.save(answer);
    }

    public Optional<ClientAnswer> findByClientAndType(UUID clientUuid, QuestionType type) {
        return repository.findByClientUuidAndQuestionTypeAndDeletedAtIsNull(clientUuid, type);
    }

    public List<ClientAnswer> findAllByClient(UUID clientUuid) {
        return repository.findAllByClientUuidAndDeletedAtIsNull(clientUuid);
    }

}
