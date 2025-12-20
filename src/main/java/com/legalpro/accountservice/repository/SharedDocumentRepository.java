package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.SharedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedDocumentRepository extends JpaRepository<SharedDocument, Long> {

    Optional<SharedDocument> findByUuidAndDeletedAtIsNull(UUID uuid);

    List<SharedDocument> findAllByLawyerUuidAndDeletedAtIsNull(UUID lawyerUuid);

    List<SharedDocument> findAllByClientUuidAndDeletedAtIsNull(UUID clientUuid);

    List<SharedDocument> findAllByDocumentUuidAndDeletedAtIsNull(UUID documentUuid);
}
