package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.CompanyInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyInviteRepository extends JpaRepository<CompanyInvite, Long>,
        JpaSpecificationExecutor<CompanyInvite>{
    Optional<CompanyInvite> findByToken(String token);
    long countByCompanyUuidAndUsedFalse(UUID companyUuid);
    long countByCompanyUuid(UUID companyUuid);
    long countByCompanyUuidAndUsedTrue(UUID companyUuid);
    long countByCompanyUuidAndUsedFalseAndExpiresAtAfter(UUID companyUuid, LocalDateTime now);

    long countByCompanyUuidAndUsedFalseAndExpiresAtBefore(UUID companyUuid, LocalDateTime now);
    Optional<CompanyInvite> findByUuid(UUID uuid);

    List<CompanyInvite> findTop3ByCompanyUuidAndUsedFalseOrderByCreatedAtDesc(UUID companyUuid);
}
