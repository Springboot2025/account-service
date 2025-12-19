package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.CategoryWithSubheadingsDto;
import com.legalpro.accountservice.dto.DocumentCategoryDto;
import com.legalpro.accountservice.dto.DocumentTemplateCenterDto;
import com.legalpro.accountservice.dto.LawyerDocumentSubheadingDto;
import com.legalpro.accountservice.entity.DocumentCategory;
import com.legalpro.accountservice.entity.DocumentTemplateCenter;
import com.legalpro.accountservice.entity.LawyerDocumentSubheading;
import com.legalpro.accountservice.mapper.DocumentCategoryMapper;
import com.legalpro.accountservice.mapper.DocumentTemplateCenterMapper;
import com.legalpro.accountservice.mapper.LawyerDocumentSubheadingMapper;
import com.legalpro.accountservice.repository.DocumentCategoryRepository;
import com.legalpro.accountservice.repository.LawyerDocumentSubheadingRepository;
import com.legalpro.accountservice.repository.DocumentTemplateCenterRepository;
import com.legalpro.accountservice.service.DocumentTemplateCenterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentTemplateCenterServiceImpl
        implements DocumentTemplateCenterService {

    private final DocumentCategoryRepository categoryRepository;
    private final LawyerDocumentSubheadingRepository subheadingRepository;
    private final DocumentTemplateCenterRepository documentRepository;

    public DocumentTemplateCenterServiceImpl(
            DocumentCategoryRepository categoryRepository,
            LawyerDocumentSubheadingRepository subheadingRepository,
            DocumentTemplateCenterRepository documentRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.subheadingRepository = subheadingRepository;
        this.documentRepository = documentRepository;
    }

    // =========================================================
    // STEP 6.1 — Fetch all system-defined categories
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public List<DocumentCategoryDto> getAllCategories() {
        return categoryRepository
                .findAllByDeletedAtIsNullOrderByDisplayOrderAsc()
                .stream()
                .map(DocumentCategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // STEP 6.2 — Fetch subheadings grouped by category (lawyer)
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public List<CategoryWithSubheadingsDto> getSubheadingsGroupedByCategory(UUID lawyerUuid) {

        // 1. Fetch all categories
        List<DocumentCategory> categories =
                categoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAsc();

        // 2. Fetch all subheadings for lawyer
        List<LawyerDocumentSubheading> subheadings =
                subheadingRepository.findAllByLawyerUuidAndDeletedAtIsNull(lawyerUuid);

        // 3. Group subheadings by category
        Map<Long, List<LawyerDocumentSubheadingDto>> subheadingsByCategory =
                subheadings.stream()
                        .map(LawyerDocumentSubheadingMapper::toDto)
                        .collect(Collectors.groupingBy(
                                LawyerDocumentSubheadingDto::getCategoryId
                        ));

        // 4. Build response
        List<CategoryWithSubheadingsDto> response = new ArrayList<>();

        for (DocumentCategory category : categories) {
            response.add(
                    CategoryWithSubheadingsDto.builder()
                            .categoryId(category.getId())
                            .categoryKey(category.getKey())
                            .categoryName(category.getDisplayName())
                            .subheadings(
                                    subheadingsByCategory.getOrDefault(
                                            category.getId(),
                                            Collections.emptyList()
                                    )
                            )
                            .build()
            );
        }

        return response;
    }

    // =========================================================
    // STEP 6.3 — Create new subheading
    // =========================================================
    @Override
    public LawyerDocumentSubheadingDto createSubheading(
            UUID lawyerUuid,
            Long categoryId,
            String subheadingName
    ) {

        // 1. Validate category
        DocumentCategory category = categoryRepository
                .findById(categoryId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid document category")
                );

        // 2. Create subheading
        LawyerDocumentSubheading subheading = LawyerDocumentSubheading.builder()
                .uuid(UUID.randomUUID())
                .lawyerUuid(lawyerUuid)
                .category(category)
                .name(subheadingName)
                .build();

        // 3. Persist
        LawyerDocumentSubheading saved = subheadingRepository.save(subheading);

        // 4. Return DTO
        return LawyerDocumentSubheadingMapper.toDto(saved);
    }

    @Override
    public List<DocumentTemplateCenterDto> uploadDocuments(
            UUID lawyerUuid,
            Long categoryId,
            Long subheadingId,
            String newSubheadingName,
            List<MultipartFile> files
    ) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }

        if (subheadingId == null && newSubheadingName == null) {
            throw new IllegalArgumentException("Either subheadingId or newSubheadingName is required");
        }

        if (subheadingId != null && newSubheadingName != null) {
            throw new IllegalArgumentException("Provide either subheadingId or newSubheadingName, not both");
        }

        // =========================================================
        // 1. Resolve subheading
        // =========================================================
        LawyerDocumentSubheading subheading;

        if (subheadingId != null) {
            subheading = subheadingRepository
                    .findByIdAndLawyerUuidAndDeletedAtIsNull(subheadingId, lawyerUuid)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Invalid subheading")
                    );
        } else {
            // Create new subheading
            DocumentCategory category = categoryRepository
                    .findById(categoryId)
                    .filter(c -> c.getDeletedAt() == null)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Invalid category")
                    );

            subheading = LawyerDocumentSubheading.builder()
                    .uuid(UUID.randomUUID())
                    .lawyerUuid(lawyerUuid)
                    .category(category)
                    .name(newSubheadingName)
                    .build();

            subheading = subheadingRepository.save(subheading);
        }

        // =========================================================
        // 2. Upload files to GCS + persist DB records
        // =========================================================
        Storage storage = StorageOptions.getDefaultInstance().getService();
        String bucketName = "legalpro-documents-template-center";

        List<DocumentTemplateCenterDto> response = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String objectName =
                        lawyerUuid + "/" +
                                subheading.getUuid() + "/" +
                                UUID.randomUUID() + "-" +
                                file.getOriginalFilename();

                BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                        .setContentType(file.getContentType())
                        .build();

                storage.create(blobInfo, file.getBytes());

                DocumentTemplateCenter document = DocumentTemplateCenter.builder()
                        .uuid(UUID.randomUUID())
                        .lawyerUuid(lawyerUuid)
                        .subheading(subheading)
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .fileUrl("gs://" + bucketName + "/" + objectName)
                        .createdAt(LocalDateTime.now())
                        .build();

                DocumentTemplateCenter saved = documentRepository.save(document);

                response.add(DocumentTemplateCenterMapper.toDto(saved));

            } catch (IOException ex) {
                throw new RuntimeException("Failed to upload document", ex);
            }
        }

        return response;
    }

    @Override
    public void deleteDocument(UUID lawyerUuid, UUID documentUuid) {

        DocumentTemplateCenter document = documentRepository
                .findByUuidAndLawyerUuidAndDeletedAtIsNull(documentUuid, lawyerUuid)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found or access denied")
                );

        document.setDeletedAt(java.time.LocalDateTime.now());
        documentRepository.save(document);
    }

    @Override
    public List<DocumentTemplateCenterDto> getDocumentsByLawyer(UUID lawyerUuid) {
        return documentRepository
                .findAllByLawyerUuidAndDeletedAtIsNull(lawyerUuid)
                .stream()
                .map(DocumentTemplateCenterMapper::toDto)
                .toList();
    }

}
