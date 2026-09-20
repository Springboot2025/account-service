package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

    Optional<SupportTicket> findByUuidAndDeletedAtIsNull(UUID uuid);

    Page<SupportTicket> findAllByCreatedByUuidAndDeletedAtIsNullOrderByCreatedAtDesc(UUID createdByUuid, Pageable pageable);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.deletedAt IS NULL")
    Long countTotal();

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.deletedAt IS NULL AND t.priority = 'URGENT' AND t.status <> 'CLOSED'")
    Long countUrgentOpen();

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.deletedAt IS NULL AND t.status = 'IN_PROGRESS'")
    Long countInProgress();

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.deletedAt IS NULL AND t.status = 'RESOLVED'")
    Long countResolved();
}
