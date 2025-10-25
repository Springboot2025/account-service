package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.ClientInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientInvoiceRepository extends JpaRepository<ClientInvoice, Long> {

    // Single invoice lookup by UUID
    Optional<ClientInvoice> findByUuid(UUID uuid);

    // All invoices for a specific lawyer (used in service)
    List<ClientInvoice> findByLawyerUuid(UUID lawyerUuid);

    // All invoices for a specific case
    List<ClientInvoice> findByCaseUuid(UUID caseUuid);

    // Filter invoices by lawyer and status
    List<ClientInvoice> findByLawyerUuidAndStatusIgnoreCase(UUID lawyerUuid, String status);
}
