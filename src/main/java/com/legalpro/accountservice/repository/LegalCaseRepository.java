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
}
