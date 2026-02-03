package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.repository.projection.CaseStatsProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalCaseRepository extends JpaRepository<LegalCase, Long> {

    Optional<LegalCase> findByUuid(UUID uuid);

    List<LegalCase> findAllByLawyerUuid(UUID lawyerUuid);

    List<LegalCase> findAllByLawyerUuidAndClientUuid(UUID lawyerUuid, UUID clientUuid);

    long countByLawyerUuid(UUID lawyerUuid);

    @Query("SELECT COUNT(c) FROM LegalCase c WHERE c.lawyerUuid = :lawyerUuid AND c.deletedAt IS NULL")
    long countActiveByLawyerUuid(UUID lawyerUuid);

    List<LegalCase> findAllByLawyerUuidAndStatusNameIgnoreCase(UUID lawyerUuid, String statusName);

    @Query("""
    SELECT s.name, COUNT(c)
    FROM CaseStatus s
    LEFT JOIN LegalCase c
      ON c.status = s AND c.lawyerUuid = :lawyerUuid
    GROUP BY s.name
    ORDER BY s.name
    """)
    List<Object[]> countCasesGroupedByStatus(UUID lawyerUuid);

    List<LegalCase> findAllByLawyerUuidAndCaseType_NameIgnoreCase(UUID lawyerUuid, String typeName);

    @Query("""
    SELECT t.name, COUNT(c.id)
    FROM CaseType t
    LEFT JOIN LegalCase c
      ON c.caseType = t AND c.lawyerUuid = :lawyerUuid
    GROUP BY t.name
    ORDER BY t.name
    """)
    List<Object[]> countCasesGroupedByType(UUID lawyerUuid);

    // ✅ NEW — avoids LazyInitializationException for status and caseType
    @Query("""
    SELECT c FROM LegalCase c
    LEFT JOIN FETCH c.status
    LEFT JOIN FETCH c.caseType
    WHERE c.lawyerUuid = :lawyerUuid
    """)
    List<LegalCase> findAllByLawyerUuidWithStatus(UUID lawyerUuid);
    boolean existsByClientUuidAndLawyerUuid(UUID clientUuid, UUID lawyerUuid);

    List<LegalCase> findAllByClientUuid(UUID clientUuid);

    @Query("""
        SELECT COUNT(c.id) > 0
        FROM LegalCase c
        JOIN c.status cs
        WHERE c.lawyerUuid = :lawyerUuid
          AND cs.name IN ('NEW', 'OPEN', 'IN_PROGRESS')
          AND c.deletedAt IS NULL
    """)
    boolean existsActiveCasesForLawyer(@Param("lawyerUuid") UUID lawyerUuid);

    // 🔹 Used for admin lawyers list (card counts)
    @Query("""
        SELECT
            SUM(CASE WHEN cs.name = 'CLOSED' THEN 1 ELSE 0 END) AS closed,
            SUM(CASE WHEN cs.name IN ('OPEN','IN_PROGRESS') THEN 1 ELSE 0 END) AS active,
            SUM(CASE WHEN cs.name = 'NEW' THEN 1 ELSE 0 END) AS pending
        FROM LegalCase c
        JOIN c.status cs
        WHERE c.lawyerUuid = :lawyerUuid
          AND c.deletedAt IS NULL
    """)
    CaseStatsProjection getCaseStatsForLawyer(@Param("lawyerUuid") UUID lawyerUuid);

    boolean existsByUuidAndLawyerUuidAndClientUuid(
            UUID caseUuid,
            UUID lawyerUuid,
            UUID clientUuid
    );

    Optional<LegalCase> findByUuidAndDeletedAtIsNull(UUID uuid);

    List<LegalCase> findAllByLawyerUuidAndDeletedAtIsNull(UUID lawyerUuid);

    @Query("""
        SELECT c
        FROM LegalCase c
        LEFT JOIN FETCH c.caseType
        WHERE c.lawyerUuid = :lawyerUuid
          AND c.deletedAt IS NULL
    """)
    List<LegalCase> findAllForLawyerWithCaseType(UUID lawyerUuid);

    @Query("""
        SELECT c FROM LegalCase c
        WHERE c.clientUuid = :clientUuid
          AND c.deletedAt IS NULL
        ORDER BY c.createdAt DESC
    """)
    List<LegalCase> findLatestCaseForClient(
            @Param("clientUuid") UUID clientUuid,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(c)
        FROM LegalCase c
        JOIN c.status s
        WHERE c.lawyerUuid = :lawyerUuid
          AND s.name = 'Active'
          AND c.deletedAt IS NULL
          AND c.createdAt >= :start
          AND c.createdAt < :end
    """)
        int countActiveCasesForPeriod(
                @Param("lawyerUuid") UUID lawyerUuid,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end
        );

    long countByLawyerUuidAndStatus_Name(UUID lawyerUuid, String statusName);
    List<LegalCase> findByLawyerUuidAndCourtDateBetween(UUID lawyerUuid, LocalDate start, LocalDate end);

    @Query("""
        SELECT COUNT(DISTINCT c.clientUuid)
        FROM LegalCase c
        WHERE c.lawyerUuid = :lawyerUuid
    """)
        long countDistinctClients(@Param("lawyerUuid") UUID lawyerUuid);

    @Query("""
        SELECT COUNT(DISTINCT c.clientUuid)
        FROM LegalCase c
        WHERE c.lawyerUuid = :lawyerUuid
          AND c.createdAt BETWEEN :start AND :end
    """)
        long countDistinctClientsByMonth(
                @Param("lawyerUuid") UUID lawyerUuid,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end
        );

    @Query("""
        SELECT COUNT(c)
        FROM LegalCase c
        WHERE c.lawyerUuid = :lawyerUuid
          AND c.status.name = :status
          AND c.createdAt BETWEEN :start AND :end
    """)
        long countCasesByStatusAndMonth(
                @Param("lawyerUuid") UUID lawyerUuid,
                @Param("status") String status,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end
        );

    @Query("""
        SELECT COUNT(c)
        FROM LegalCase c
        WHERE c.lawyerUuid = :lawyerUuid
          AND c.followUp IS NOT NULL
          AND c.followUp <> ''
    """)
        long countPendingReminders(@Param("lawyerUuid") UUID lawyerUuid);

    long countByClientUuidAndStatus_NameNotInAndDeletedAtIsNull(
            UUID clientUuid,
            List<String> excludedStatuses
    );

    long countByClientUuidAndCreatedAtBetweenAndDeletedAtIsNull(
            UUID clientUuid,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByClientUuidAndCourtDateBetweenAndDeletedAtIsNull(
            UUID clientUuid,
            LocalDate start,
            LocalDate end
    );

    List<LegalCase> findAllByLawyerUuidAndCasePriority(UUID lawyerUuid, int priority);
    List<LegalCase> findAllByLawyerUuidAndCaseFinalStatus(UUID lawyerUuid, int finalStatus);

    Optional<LegalCase> findByQuoteUuid(UUID quoteUuid);
}
