package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    // total active subscribers
    @Query("""
        SELECT COUNT(us)
        FROM UserSubscription us
        WHERE us.status = 1
        AND us.deletedAt IS NULL
    """)
    Long countActiveSubscribers();


    // cancelled subscriptions
    @Query("""
        SELECT COUNT(us)
        FROM UserSubscription us
        WHERE us.status = 2
        AND us.deletedAt IS NULL
    """)
    Long countCancelled();


    // new subscriptions this month
    @Query("""
        SELECT COUNT(us)
        FROM UserSubscription us
        WHERE us.createdAt >= :startOfMonth
        AND us.deletedAt IS NULL
    """)
    Long countNewSince(LocalDateTime startOfMonth);


    // all subscriptions (used for retention calculation)
    @Query("""
        SELECT COUNT(us)
        FROM UserSubscription us
        WHERE us.deletedAt IS NULL
    """)
    Long countTotalSubscriptions();


    // recent subscribers list (API 3)
    List<UserSubscription> findTop10ByDeletedAtIsNullOrderByCreatedAtDesc();


    // for plan breakdown (API 2 later)
    List<UserSubscription> findByPlanIdAndStatus(Long planId, Integer status);
    Optional<UserSubscription> findByUuid(UUID uuid);

    // the payer's current subscription (latest row)
    Optional<UserSubscription> findFirstByUserUuidAndDeletedAtIsNullOrderByIdDesc(UUID userUuid);

    // active subscriptions with a saved PayWay card whose billing date has come
    @Query("""
        SELECT us.id
        FROM UserSubscription us
        WHERE us.status = 1
        AND us.deletedAt IS NULL
        AND us.paywayCustomerNumber IS NOT NULL
        AND us.renewsAt <= :now
    """)
    List<Long> findIdsDueForRenewal(LocalDateTime now);
}
