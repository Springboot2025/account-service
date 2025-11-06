package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.LawyerInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LawyerInvoiceRepository extends JpaRepository<LawyerInvoice, Long> {

    // Single invoice lookup by UUID
    Optional<LawyerInvoice> findByUuid(UUID uuid);

    // All invoices for a specific lawyer (used in service)
    List<LawyerInvoice> findByLawyerUuid(UUID lawyerUuid);

    // All invoices for a specific case
    List<LawyerInvoice> findByCaseUuid(UUID caseUuid);

    // Filter invoices by lawyer and status
    List<LawyerInvoice> findByLawyerUuidAndStatusIgnoreCase(UUID lawyerUuid, String status);

    Optional<LawyerInvoice> findByUuidAndLawyerUuid(UUID uuid, UUID lawyerUuid);
}
