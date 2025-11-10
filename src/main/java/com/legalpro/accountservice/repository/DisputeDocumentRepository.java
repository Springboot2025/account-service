package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.DisputeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeDocumentRepository extends JpaRepository<DisputeDocument, Long> {
    List<DisputeDocument> findAllByDisputeUuid(UUID disputeUuid);
}
