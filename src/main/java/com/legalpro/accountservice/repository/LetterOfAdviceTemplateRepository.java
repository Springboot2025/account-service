package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.LetterOfAdviceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LetterOfAdviceTemplateRepository extends JpaRepository<LetterOfAdviceTemplate, Long> {

    List<LetterOfAdviceTemplate> findAllByLawyerUuidAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID lawyerUuid);

    Optional<LetterOfAdviceTemplate> findByUuidAndLawyerUuidAndDeletedAtIsNull(UUID uuid, UUID lawyerUuid);
}
