package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {

    // Fetch all active categories ordered for UI
    List<DocumentCategory> findAllByDeletedAtIsNullOrderByDisplayOrderAsc();

    // Fetch category by key (CASE_RELATED, FINANCIAL, etc.)
    Optional<DocumentCategory> findByKeyAndDeletedAtIsNull(String key);
}
