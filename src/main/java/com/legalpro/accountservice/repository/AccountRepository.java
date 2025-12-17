package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.enums.AdminLawyerStatus;
import com.legalpro.accountservice.repository.projection.CaseStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    @Query("SELECT a FROM Account a JOIN a.roles r WHERE r.name = :roleName")
    List<Account> findByRoleName(String roleName);

    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Account> findByUuid(UUID uuid);

    Optional<Account> findByVerificationToken(UUID token);
    Optional<Account> findByForgotPasswordToken(UUID token);

    List<Account> findByCompanyUuid(UUID companyUuid);

    // Get all accounts that are companies
    List<Account> findByIsCompanyTrue();

    List<Account> findByUuidIn(Set<UUID> uuids);

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        WHERE a.role = 'LAWYER'
          AND a.deletedAt IS NULL
    """)
    long countLawyers();

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        WHERE a.role = 'LAWYER'
          AND a.isActive = true
          AND a.deletedAt IS NULL
    """)
    long countActiveLawyers();

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        WHERE a.isCompany = true
          AND a.deletedAt IS NULL
    """)
    long countFirms();

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        WHERE a.isCompany = true
          AND a.isActive = true
          AND a.deletedAt IS NULL
    """)
    long countActiveFirms();

    @Query("""
        SELECT a
        FROM Account a
        WHERE a.role = 'LAWYER'
          AND a.deletedAt IS NULL
          AND (:active IS NULL OR a.isActive = :active)
          AND (
               :search IS NULL OR
               LOWER(a.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<Account> searchLawyers(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("""
        SELECT
          SUM(CASE WHEN cs.name = 'CLOSED' THEN 1 ELSE 0 END),
          SUM(CASE WHEN cs.name IN ('OPEN','IN_PROGRESS') THEN 1 ELSE 0 END),
          SUM(CASE WHEN cs.name = 'NEW' THEN 1 ELSE 0 END)
        FROM Case c
        JOIN c.status cs
        WHERE c.lawyerUuid = :lawyerUuid
    """)
    CaseStatsProjection getCaseStatsForLawyer(UUID lawyerUuid);


    @Query("""
        SELECT a
        FROM Account a
        LEFT JOIN a.specializations s
        WHERE a.role = 'LAWYER'
          AND (
               :search IS NULL OR
               LOWER(a.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
          )
          AND (
               :category IS NULL OR s.name = :category
          )
          AND (
               :status = 'ALL'
            OR (:status = 'ACTIVE' AND a.isActive = true AND a.deletedAt IS NULL)
            OR (:status = 'DEACTIVATED' AND a.isActive = false AND a.deletedAt IS NULL)
            OR (:status = 'DELETED' AND a.deletedAt IS NOT NULL)
          )
    """)
    Page<Account> findLawyers(
            String search,
            AdminLawyerStatus status,
            String category,
            Pageable pageable
    );

}
