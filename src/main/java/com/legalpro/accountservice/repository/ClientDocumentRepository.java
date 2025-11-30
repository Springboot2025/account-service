package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.ClientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientDocumentRepository extends JpaRepository<ClientDocument, Long> {

    // Fetch all active (non-deleted) documents for a client
    List<ClientDocument> findAllByClientUuidAndDeletedAtIsNull(UUID clientUuid);

    // Fetch all active docs of a client that belong to a specific lawyer
    List<ClientDocument> findAllByClientUuidAndLawyerUuidAndDeletedAtIsNull(UUID clientUuid, UUID lawyerUuid);

    // Fetch single active doc
    Optional<ClientDocument> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByClientUuidAndDocumentType(UUID clientUuid, String documentType);

    List<ClientDocument> findByClientUuidAndDocumentType(UUID clientUuid, String documentType);
    List<ClientDocument> findByClientUuidAndDeletedAtIsNull(UUID clientUuid);
    List<ClientDocument> findAllByClientUuidAndCaseUuidAndDeletedAtIsNull(UUID clientUuid, UUID caseUuid);
}
