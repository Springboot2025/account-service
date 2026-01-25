package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.DocumentTemplateCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentTemplateCenterRepository
        extends JpaRepository<DocumentTemplateCenter, Long> {

    // Fetch all active documents under a subheading
    List<DocumentTemplateCenter> findAllByLawyerUuidAndSubheading_IdAndDeletedAtIsNull(
            UUID lawyerUuid,
            Long subheadingId
    );

    // Fetch all active documents for a lawyer (future use)
    List<DocumentTemplateCenter> findAllByLawyerUuidAndDeletedAtIsNull(UUID lawyerUuid);

    // Fetch a document safely for delete/share
    Optional<DocumentTemplateCenter> findByIdAndLawyerUuidAndDeletedAtIsNull(
            Long id,
            UUID lawyerUuid
    );

    Optional<DocumentTemplateCenter>
    findByUuidAndLawyerUuidAndDeletedAtIsNull(UUID uuid, UUID lawyerUuid);

    List<DocumentTemplateCenter>
    findAllBySubheadingIdAndLawyerUuidAndDeletedAtIsNull(
            Long subheadingId,
            UUID lawyerUuid
    );

    boolean existsByUuidAndLawyerUuidAndDeletedAtIsNull(UUID uuid, UUID lawyerUuid);

    long countByLawyerUuidAndDeletedAtIsNull(UUID lawyerUuid);

    long countByLawyerUuidAndCreatedAtBetweenAndDeletedAtIsNull(
            UUID lawyerUuid,
            LocalDateTime start,
            LocalDateTime end
    );
}
