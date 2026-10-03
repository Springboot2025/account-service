package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.PayWayTransaction;
import com.legalpro.accountservice.enums.PayWayTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayWayTransactionRepository extends JpaRepository<PayWayTransaction, Long> {

    Optional<PayWayTransaction> findByUuid(UUID uuid);

    // renewal job: was this invoice already attempted recently? (one try per day)
    boolean existsByInvoiceUuidAndCreatedAtAfter(UUID invoiceUuid, LocalDateTime after);

    // billing history: the approved payment behind each paid invoice
    List<PayWayTransaction> findByInvoiceUuidInAndStatus(Collection<UUID> invoiceUuids, PayWayTransactionStatus status);
}
