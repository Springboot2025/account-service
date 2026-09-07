package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> {

    List<RefreshSession> findAllByFamilyId(String familyId);

    @Modifying
    @Query("UPDATE RefreshSession r SET r.revoked = true WHERE r.familyId = :familyId")
    void revokeFamily(@Param("familyId") String familyId);

    @Modifying
    @Query("DELETE FROM RefreshSession r WHERE r.expiresAt < :now")
    void deleteExpiredBefore(@Param("now") LocalDateTime now);
}
