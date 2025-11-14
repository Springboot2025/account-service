package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.CompanyInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyInviteRepository extends JpaRepository<CompanyInvite, Long> {
    Optional<CompanyInvite> findByToken(String token);
}
