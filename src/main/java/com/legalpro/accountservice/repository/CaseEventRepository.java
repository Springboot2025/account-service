package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.CaseEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

    List<CaseEvent> findAllByCaseUuidAndDeletedAtIsNullOrderByEventDateDesc(UUID caseUuid);

    @Query("""
        SELECT e
        FROM CaseEvent e
        JOIN LegalCase c ON c.uuid = e.caseUuid
        WHERE c.lawyerUuid = :lawyerUuid
          AND e.eventDate >= :now
          AND e.deletedAt IS NULL
        ORDER BY e.eventDate ASC
    """)
        List<CaseEvent> findUpcomingEvents(
                @Param("lawyerUuid") UUID lawyerUuid,
                @Param("now") LocalDateTime now,
                Pageable pageable
        );

}
