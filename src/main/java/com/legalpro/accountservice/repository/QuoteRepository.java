package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Quote;
import com.legalpro.accountservice.enums.QuoteStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    Optional<Quote> findByUuid(UUID uuid);
    Optional<Quote> findByUuidAndClientUuid(UUID uuid, UUID clientUuid);

    List<Quote> findByLawyerUuid(UUID lawyerUuid);

    List<Quote> findByClientUuid(UUID clientUuid);

    List<Quote> findByLawyerUuidAndStatus(UUID lawyerUuid, QuoteStatus status);

    List<Quote> findByClientUuidAndStatus(UUID clientUuid, QuoteStatus status);

    List<Quote> findByLawyerUuidAndClientUuid(UUID lawyerUuid, UUID clientUuid);
    List<Quote> findByClientUuidAndLawyerUuid(UUID clientUuid, UUID lawyerUuid);

    @Query(value = """
        SELECT DISTINCT ON (q.client_uuid) q.*
        FROM quotes q
        WHERE q.lawyer_uuid = :lawyerUuid
        ORDER BY q.client_uuid, q.created_at DESC
    """, nativeQuery = true)
    List<Quote> findLatestQuotesForLawyer(UUID lawyerUuid);

    @Query("""
        SELECT COUNT(q)
        FROM Quote q
        WHERE q.lawyerUuid = :lawyerUuid
          AND q.status = 'REQUESTED'
          AND q.deletedAt IS NULL
          AND q.createdAt >= :start
          AND q.createdAt < :end
    """)
        int countNewRequestsForPeriod(
                @Param("lawyerUuid") UUID lawyerUuid,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end
        );

    @Query("""
        SELECT q
        FROM Quote q
        WHERE q.lawyerUuid = :lawyerUuid
          AND q.deletedAt IS NULL
        ORDER BY q.createdAt DESC
    """)
        List<Quote> findRecentQuotesForLawyer(UUID lawyerUuid, Pageable pageable);


}
