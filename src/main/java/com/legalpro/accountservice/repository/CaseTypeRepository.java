package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Long> {

    Optional<CaseType> findByNameIgnoreCase(String name);
}
