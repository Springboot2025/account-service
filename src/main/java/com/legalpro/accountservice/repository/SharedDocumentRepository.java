package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.dto.ClientLetterDto;
import com.legalpro.accountservice.dto.ClientLetterView;
import com.legalpro.accountservice.entity.SharedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT sd, c, d
        FROM SharedDocument sd
        JOIN LegalCase c ON c.uuid = sd.caseUuid
        JOIN DocumentTemplateCenter d ON d.uuid = sd.documentUuid
        WHERE sd.clientUuid = :clientUuid
          AND sd.deletedAt IS NULL
          AND c.deletedAt IS NULL
        ORDER BY sd.createdAt DESC
    """)
        List<Object[]> findLettersForClient(UUID clientUuid);



    long countByLawyerUuidAndDeletedAtIsNull(UUID lawyerUuid);
}
