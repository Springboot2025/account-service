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

    @Query(value = """
        SELECT COUNT(DISTINCT a.id)
        FROM accounts a
        JOIN account_roles ar ON a.id = ar.account_id
        JOIN roles r ON ar.role_id = r.id
        WHERE r.name = 'Lawyer'
          AND a.removed_at IS NULL
    """, nativeQuery = true)
    long countLawyers();

    @Query(value = """
        SELECT COUNT(DISTINCT a.id)
        FROM accounts a
        JOIN account_roles ar ON a.id = ar.account_id
        JOIN roles r ON ar.role_id = r.id
        WHERE r.name = 'Lawyer'
          AND a.is_active = true
          AND a.removed_at IS NULL
    """, nativeQuery = true)
    long countActiveLawyers();

    @Query(value = """
        SELECT COUNT(*)
        FROM accounts a
        WHERE a.is_company = true
          AND a.removed_at IS NULL
    """, nativeQuery = true)
    long countFirms();

    @Query(value = """
        SELECT COUNT(*)
        FROM accounts a
        WHERE a.is_company = true
          AND a.is_active = true
          AND a.removed_at IS NULL
    """, nativeQuery = true)
    long countActiveFirms();

    /* =========================
       ADMIN – LAWYERS LIST
       (NATIVE + JSONB + PAGINATION)
       ========================= */

    @Query(
            value = """
        SELECT DISTINCT a.*
        FROM accounts a
        JOIN account_roles ar ON a.id = ar.account_id
        JOIN roles r ON ar.role_id = r.id
        WHERE LOWER(r.name) = 'lawyer'
          AND a.removed_at IS NULL
          AND (
               :search IS NULL OR
               LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.personal_details->>'firstName') LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.personal_details->>'lastName') LIKE LOWER(CONCAT('%', :search, '%'))
          )
          AND (
               :status IS NULL OR
               (:status = 'ACTIVE' AND a.is_active = true AND a.removed_at IS NULL) OR
               (:status = 'DEACTIVATED' AND a.is_active = false AND a.removed_at IS NULL) OR
               (:status = 'DELETED' AND a.removed_at IS NOT NULL)
          )
        ORDER BY a.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT a.id)
        FROM accounts a
        JOIN account_roles ar ON a.id = ar.account_id
        JOIN roles r ON ar.role_id = r.id
        WHERE LOWER(r.name) = 'lawyer'
          AND a.removed_at IS NULL
          AND (
               :search IS NULL OR
               LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.personal_details->>'firstName') LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(a.personal_details->>'lastName') LIKE LOWER(CONCAT('%', :search, '%'))
          )
          AND (
               :status IS NULL OR
               (:status = 'ACTIVE' AND a.is_active = true AND a.removed_at IS NULL) OR
               (:status = 'DEACTIVATED' AND a.is_active = false AND a.removed_at IS NULL) OR
               (:status = 'DELETED' AND a.removed_at IS NOT NULL)
          )
        """,
            nativeQuery = true
    )
    Page<Account> findLawyers(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );


}
