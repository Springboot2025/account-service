package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.ClientAnswer;
import com.legalpro.accountservice.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientAnswerRepository extends JpaRepository<ClientAnswer, Long> {
    Optional<ClientAnswer> findByClientUuidAndQuestionTypeAndDeletedAtIsNull(UUID clientUuid, QuestionType type);

    List<ClientAnswer> findAllByClientUuidAndDeletedAtIsNull(UUID clientUuid);

}
