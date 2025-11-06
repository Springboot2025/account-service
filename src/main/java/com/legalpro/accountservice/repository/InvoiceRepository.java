package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Single invoice lookup by UUID
    Optional<Invoice> findByUuid(UUID uuid);

    // All invoices for a specific lawyer (used in service)
    List<Invoice> findByLawyerUuid(UUID lawyerUuid);

    List<Invoice> findByClientUuid(UUID clientUuid);

    // All invoices for a specific case
    List<Invoice> findByCaseUuid(UUID caseUuid);

    // Filter invoices by lawyer and status
    List<Invoice> findByLawyerUuidAndStatusIgnoreCase(UUID lawyerUuid, String status);

    Optional<Invoice> findByUuidAndLawyerUuid(UUID uuid, UUID lawyerUuid);
}
