package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.LawyerDocumentSubheading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LawyerDocumentSubheadingRepository
        extends JpaRepository<LawyerDocumentSubheading, Long> {

    // Fetch all active subheadings for a lawyer
    List<LawyerDocumentSubheading> findAllByLawyerUuidAndDeletedAtIsNull(UUID lawyerUuid);

    // Fetch subheadings for a lawyer under a specific category
    List<LawyerDocumentSubheading> findAllByLawyerUuidAndCategory_IdAndDeletedAtIsNull(
            UUID lawyerUuid,
            Long categoryId
    );

    // Fetch a specific subheading by id (ownership + soft delete check)
    Optional<LawyerDocumentSubheading> findByIdAndLawyerUuidAndDeletedAtIsNull(
            Long id,
            UUID lawyerUuid
    );
}
