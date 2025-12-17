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

public interface AccountRepository
        extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    /* =========================
       BASIC FINDERS
       ========================= */

    @Query("""
        SELECT a
        FROM Account a
        JOIN a.roles r
        WHERE r.name = :roleName
    """)
    List<Account> findByRoleName(@Param("roleName") String roleName);

    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Account> findByUuid(UUID uuid);

    Optional<Account> findByVerificationToken(UUID token);
    Optional<Account> findByForgotPasswordToken(UUID token);

    List<Account> findByCompanyUuid(UUID companyUuid);

    List<Account> findByIsCompanyTrue();

    List<Account> findByUuidIn(Set<UUID> uuids);

    /* =========================
       DASHBOARD COUNTS
       ========================= */

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        JOIN a.roles r
        WHERE r.name = 'Lawyer'
          AND a.removedAt IS NULL
    """)
    long countLawyers();

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        JOIN a.roles r
        WHERE r.name = 'Lawyer'
          AND a.isActive = true
          AND a.removedAt IS NULL
    """)
    long countActiveLawyers();

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        WHERE a.isCompany = true
          AND a.removedAt IS NULL
    """)
    long countFirms();

    @Query("""
        SELECT COUNT(a)
        FROM Account a
        WHERE a.isCompany = true
          AND a.isActive = true
          AND a.removedAt IS NULL
    """)
    long countActiveFirms();

    /* =========================
       ADMIN – LAWYER LIST
       ========================= */

    @Query("""
        SELECT a
        FROM Account a
        JOIN a.roles r
        WHERE r.name = 'Lawyer'
          AND (:search IS NULL OR
               LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.personalDetails->>'firstName') LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.personalDetails->>'lastName') LIKE LOWER(CONCAT('%', :search, '%'))
          )
          AND (:status IS NULL OR
               (:status = 'ACTIVE' AND a.isActive = true AND a.removedAt IS NULL) OR
               (:status = 'DEACTIVATED' AND a.isActive = false AND a.removedAt IS NULL) OR
               (:status = 'DELETED' AND a.removedAt IS NOT NULL)
          )
    """)
    Page<Account> findLawyers(
            @Param("search") String search,
            @Param("status") AdminLawyerStatus status,
            Pageable pageable
    );

}
