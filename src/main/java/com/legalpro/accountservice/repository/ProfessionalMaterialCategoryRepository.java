package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.ProfessionalMaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalMaterialCategoryRepository
        extends JpaRepository<ProfessionalMaterialCategory, Long> {

    Optional<ProfessionalMaterialCategory> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    List<ProfessionalMaterialCategory> findAllByDeletedAtIsNull();
    Optional<ProfessionalMaterialCategory> findByIdAndDeletedAtIsNull(Long id);
}
