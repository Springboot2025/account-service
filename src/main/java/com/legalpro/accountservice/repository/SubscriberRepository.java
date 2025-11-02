package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    Optional<Subscriber> findByEmail(String email);
    Optional<Subscriber> findByUuid(UUID uuid);
}
