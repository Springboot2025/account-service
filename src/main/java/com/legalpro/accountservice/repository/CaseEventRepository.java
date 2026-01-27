package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.CaseEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

    List<CaseEvent> findAllByCaseUuidAndDeletedAtIsNullOrderByEventDateDesc(UUID caseUuid);
    @Query("""
        SELECT e
        FROM CaseEvent e
        WHERE e.caseUuid IN (
            SELECT c.uuid
            FROM LegalCase c
            WHERE c.lawyerUuid = :lawyerUuid
              AND c.deletedAt IS NULL
        )
          AND e.eventDate >= :today
          AND e.deletedAt IS NULL
        ORDER BY e.eventDate ASC
    """)
        List<CaseEvent> findUpcomingEvents(
                @Param("lawyerUuid") UUID lawyerUuid,
                @Param("today") LocalDate today,
                Pageable pageable
        );

    Optional<CaseEvent> findByUuidAndDeletedAtIsNull(UUID uuid);

    @Modifying
    @Query("UPDATE CaseEvent ce SET ce.deletedAt = CURRENT_TIMESTAMP WHERE ce.uuid = :uuid")
    void softDeleteByUuid(UUID uuid);

    @Query("""
        SELECT e
        FROM CaseEvent e
        JOIN LegalCase c ON c.uuid = e.caseUuid
        WHERE c.clientUuid = :clientUuid
          AND e.deletedAt IS NULL
          AND c.deletedAt IS NULL
        ORDER BY e.relatedDate DESC
    """)
        List<CaseEvent> findUpcomingEventsForClient(
                UUID clientUuid,
                Pageable pageable
        );
}
