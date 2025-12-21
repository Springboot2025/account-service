package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ProfessionalMaterialCategoryDto;
import com.legalpro.accountservice.dto.ProfessionalMaterialResponseDto;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.ProfessionalMaterial;
import com.legalpro.accountservice.entity.ProfessionalMaterialCategory;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.ProfessionalMaterialCategoryRepository;
import com.legalpro.accountservice.repository.ProfessionalMaterialRepository;
import com.legalpro.accountservice.service.ProfessionalMaterialService;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProfessionalMaterialServiceImpl implements ProfessionalMaterialService {

    private static final String BUCKET_NAME = "legalpro-professional-materials";
    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com";

    private final ProfessionalMaterialRepository materialRepository;
    private final ProfessionalMaterialCategoryRepository categoryRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final Storage storage;

    public ProfessionalMaterialServiceImpl(
            ProfessionalMaterialRepository materialRepository,
            ProfessionalMaterialCategoryRepository categoryRepository,
            LegalCaseRepository legalCaseRepository
    ) {
        this.materialRepository = materialRepository;
        this.categoryRepository = categoryRepository;
        this.legalCaseRepository = legalCaseRepository;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    // =========================================================
    // Upload Professional Material
    // =========================================================
    @Override
    @Transactional
    public ProfessionalMaterialResponseDto uploadProfessionalMaterial(
            UUID lawyerUuid,
            UUID caseUuid,
            Long documentCatId,
            String followUp,
            String description,
            MultipartFile file
    ) throws IOException {

        // -----------------------------------------------------
        // 1️⃣ Validate case ownership
        // -----------------------------------------------------
        LegalCase legalCase = legalCaseRepository
                .findByUuidAndDeletedAtIsNull(caseUuid)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new IllegalStateException("You are not allowed to upload materials for this case");
        }

        // -----------------------------------------------------
        // 2️⃣ Validate category
        // -----------------------------------------------------
        ProfessionalMaterialCategory category = categoryRepository
                .findById(documentCatId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Invalid professional material category"));

        // -----------------------------------------------------
        // 3️⃣ Upload file to GCS
        // -----------------------------------------------------
        UUID materialUuid = UUID.randomUUID();

        String objectName =
                lawyerUuid + "/" +
                        caseUuid + "/" +
                        materialUuid + "-" + file.getOriginalFilename();

        BlobInfo blobInfo = BlobInfo.newBuilder(BUCKET_NAME, objectName)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        String gsUrl = "gs://" + BUCKET_NAME + "/" + objectName;

        // -----------------------------------------------------
        // 4️⃣ Persist DB record
        // -----------------------------------------------------
        ProfessionalMaterial material = ProfessionalMaterial.builder()
                .uuid(materialUuid)
                .caseUuid(caseUuid)
                .category(category)
                .followUp(followUp)
                .description(description)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileUrl(gsUrl)
                .createdAt(LocalDateTime.now())
                .build();

        materialRepository.save(material);

        // -----------------------------------------------------
        // 5️⃣ Build response (PUBLIC URL)
        // -----------------------------------------------------
        String publicUrl = GCS_PUBLIC_BASE + "/" + BUCKET_NAME + "/" + objectName;

        ProfessionalMaterialResponseDto response = new ProfessionalMaterialResponseDto();
        response.setUuid(material.getUuid());
        response.setCaseUuid(caseUuid);
        response.setFollowUp(material.getFollowUp());
        response.setDescription(material.getDescription());
        response.setFileName(material.getFileName());
        response.setFileType(material.getFileType());
        response.setFileUrl(publicUrl);
        response.setCreatedAt(material.getCreatedAt());
        response.setCategory(
                new ProfessionalMaterialCategoryDto(
                        category.getId(),
                        category.getName()
                )
        );

        return response;
    }
}
