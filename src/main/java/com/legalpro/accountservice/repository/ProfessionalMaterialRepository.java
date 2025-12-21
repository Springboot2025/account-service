package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.ProfessionalMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalMaterialRepository
        extends JpaRepository<ProfessionalMaterial, Long> {

    Optional<ProfessionalMaterial> findByUuidAndDeletedAtIsNull(UUID uuid);

    List<ProfessionalMaterial> findAllByCaseUuidAndDeletedAtIsNull(UUID caseUuid);
}
