package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.LetterOfAdviceChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LetterOfAdviceChangeRequestRepository extends JpaRepository<LetterOfAdviceChangeRequest, Long> {

    List<LetterOfAdviceChangeRequest> findAllByDocumentUuidOrderByCreatedAtDesc(UUID documentUuid);

    Optional<LetterOfAdviceChangeRequest> findByUuidAndDocumentUuid(UUID uuid, UUID documentUuid);

    List<LetterOfAdviceChangeRequest> findAllByDocumentUuidAndReadAtIsNull(UUID documentUuid);

    long countByDocumentUuid(UUID documentUuid);

    long countByDocumentUuidAndReadAtIsNull(UUID documentUuid);
}
