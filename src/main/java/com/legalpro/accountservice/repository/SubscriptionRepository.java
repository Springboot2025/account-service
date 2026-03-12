package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUuid(UUID uuid);

    List<Subscription> findAllByRemovedAtIsNullOrderByIdAsc();

}