package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseStatusRepository extends JpaRepository<CaseStatus, Long> {
    Optional<CaseStatus> findByName(String name);
}
