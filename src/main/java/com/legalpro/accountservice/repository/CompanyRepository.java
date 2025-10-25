package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByUuid(UUID uuid);

    List<Company> findByNameContainingIgnoreCase(String name);
}
