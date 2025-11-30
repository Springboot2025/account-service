package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.CourtSupportMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtSupportMaterialRepository extends JpaRepository<CourtSupportMaterial, Long> {

    List<CourtSupportMaterial> findAllByClientUuidAndDeletedAtIsNull(UUID clientUuid);

    Optional<CourtSupportMaterial> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByClientUuidAndFileName(UUID clientUuid, String fileName);
    List<CourtSupportMaterial> findByClientUuidAndDeletedAtIsNull(UUID clientUuid);
    List<CourtSupportMaterial> findAllByClientUuidAndCaseUuidAndDeletedAtIsNull(UUID clientUuid, UUID caseUuid);
}
