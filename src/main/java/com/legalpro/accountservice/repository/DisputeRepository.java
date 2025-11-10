package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    Optional<Dispute> findByUuid(UUID uuid);
}
