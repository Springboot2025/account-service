package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.ProfessionalMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalMaterialRepository
        extends JpaRepository<ProfessionalMaterial, Long> {

    Optional<ProfessionalMaterial> findByUuidAndDeletedAtIsNull(UUID uuid);

    List<ProfessionalMaterial> findAllByCaseUuidAndDeletedAtIsNull(UUID caseUuid);

    @Query("""
        SELECT pm
        FROM ProfessionalMaterial pm
        JOIN FETCH pm.category c
        WHERE pm.caseUuid = :caseUuid
          AND pm.deletedAt IS NULL
          AND c.deletedAt IS NULL
        ORDER BY c.id ASC, pm.createdAt DESC
    """)
    List<ProfessionalMaterial> findAllByCaseUuid(
            @Param("caseUuid") UUID caseUuid
    );
}
