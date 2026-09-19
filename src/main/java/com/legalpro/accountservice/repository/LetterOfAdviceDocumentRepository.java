package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.LetterOfAdviceDocument;
import com.legalpro.accountservice.enums.LetterOfAdviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LetterOfAdviceDocumentRepository extends JpaRepository<LetterOfAdviceDocument, Long> {

    // The one letter currently being worked on for a case (older ones are
    // superseded, not deleted).
    Optional<LetterOfAdviceDocument> findByCaseUuidAndDeletedAtIsNullAndSupersededAtIsNull(UUID caseUuid);

    List<LetterOfAdviceDocument> findAllByCaseUuidAndDeletedAtIsNullOrderByCreatedAtAsc(UUID caseUuid);

    List<LetterOfAdviceDocument> findAllByClientUuidAndStatusInAndDeletedAtIsNullOrderBySentToClientAtDesc(
            UUID clientUuid, Collection<LetterOfAdviceStatus> statuses);

    Optional<LetterOfAdviceDocument> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<LetterOfAdviceDocument> findByUuidAndLawyerUuidAndDeletedAtIsNull(UUID uuid, UUID lawyerUuid);

    Optional<LetterOfAdviceDocument> findByUuidAndClientUuidAndDeletedAtIsNull(UUID uuid, UUID clientUuid);
}
