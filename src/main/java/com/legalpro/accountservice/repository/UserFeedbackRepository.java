package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    Optional<UserFeedback> findByUuid(UUID uuid);

    @Query("""
        SELECT f FROM UserFeedback f
        WHERE f.userUuid = :userUuid AND f.deletedAt IS NULL
        ORDER BY f.createdAt DESC
    """)
    List<UserFeedback> findByUserUuid(@Param("userUuid") UUID userUuid);

    @Query("""
        SELECT f FROM UserFeedback f
        WHERE f.isPublic = true AND f.deletedAt IS NULL
        ORDER BY f.createdAt DESC
    """)
    List<UserFeedback> findAllPublic();
}
