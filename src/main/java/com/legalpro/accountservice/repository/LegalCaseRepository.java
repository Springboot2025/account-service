package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.LegalCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
    SELECT t.name, COUNT(c)
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

}
