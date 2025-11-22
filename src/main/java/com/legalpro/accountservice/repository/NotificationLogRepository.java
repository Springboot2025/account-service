package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserUuidOrderBySentAtDesc(UUID userUuid);
    @Modifying
    @Query("UPDATE NotificationLog n SET n.isRead = true WHERE n.userUuid = :userUuid")
    void markAllAsReadByUser(@Param("userUuid") UUID userUuid);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.deletedAt = CURRENT_TIMESTAMP WHERE n.id = :id")
    void softDeleteById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.deletedAt = CURRENT_TIMESTAMP WHERE n.userUuid = :userUuid")
    void softDeleteByUser(@Param("userUuid") UUID userUuid);

    boolean existsByIdAndUserUuid(Long id, UUID userUuid);

    @Modifying
    @Query("DELETE FROM NotificationLog n WHERE n.id = :id AND n.userUuid = :userUuid")
    void deleteByIdAndUserUuid(@Param("id") Long id, @Param("userUuid") UUID userUuid);

    @Modifying
    @Query("DELETE FROM NotificationLog n WHERE n.userUuid = :userUuid")
    void deleteAllByUserUuid(@Param("userUuid") UUID userUuid);

}