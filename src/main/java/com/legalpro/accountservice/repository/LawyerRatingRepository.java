package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.LawyerRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LawyerRatingRepository extends JpaRepository<LawyerRating, Long> {

    Optional<LawyerRating> findByUuid(UUID uuid);

    Optional<LawyerRating> findByLawyerUuidAndClientUuid(UUID lawyerUuid, UUID clientUuid);

    List<LawyerRating> findAllByLawyerUuid(UUID lawyerUuid);

    List<LawyerRating> findAllByClientUuid(UUID clientUuid);

    @Query("SELECT AVG(lr.rating) FROM LawyerRating lr WHERE lr.lawyerUuid = :lawyerUuid AND lr.deletedAt IS NULL")
    BigDecimal findAverageRatingByLawyerUuid(UUID lawyerUuid);

    @Query("""
        SELECT r.lawyerUuid, CAST(AVG(r.rating) AS bigdecimal) 
        FROM LawyerRating r 
        WHERE r.lawyerUuid IN :lawyerUuids 
        GROUP BY r.lawyerUuid
    """)
    List<Object[]> getAverageRatingsForLawyers(List<UUID> lawyerUuids);

    @Query("""
        SELECT COUNT(r)
        FROM LawyerRating r
        WHERE r.deletedAt IS NULL
    """)
    Long countTotalReviews();

    @Query("""
        SELECT AVG(r.rating)
        FROM LawyerRating r
        WHERE r.deletedAt IS NULL
    """)
    BigDecimal findPlatformAverageRating();

    @Query("""
        SELECT COUNT(r)
        FROM LawyerRating r
        WHERE r.rating >= 4
          AND r.deletedAt IS NULL
    """)
    Long countPositiveReviews();

    @Query("""
        SELECT COUNT(r)
        FROM LawyerRating r
        WHERE r.createdAt >= :sinceDate
          AND r.deletedAt IS NULL
    """)
    Long countReviewsSince(LocalDateTime sinceDate);

    @Query("""
    SELECT r
    FROM LawyerRating r
    WHERE r.deletedAt IS NULL
    ORDER BY r.createdAt DESC
    """)
    List<LawyerRating> findAllActiveReviews();

    Page<LawyerRating> findAllByDeletedAtIsNull(Pageable pageable);
}
