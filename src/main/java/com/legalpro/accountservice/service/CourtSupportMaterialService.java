package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.CourtSupportMaterial;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.repository.CourtSupportMaterialRepository;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CourtSupportMaterialService {

    private final CourtSupportMaterialRepository repository;
    private final LegalCaseRepository legalCaseRepository;
    private final ActivityLogService activityLogService;
    private final Storage storage;
    private final String bucketName = "legalpro-client-docs-au"; // ✅ dedicated bucket
    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com";

    public CourtSupportMaterialService(CourtSupportMaterialRepository repository,
                                       LegalCaseRepository legalCaseRepository,
                                       ActivityLogService activityLogService) {
        this.repository = repository;
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.legalCaseRepository = legalCaseRepository;
        this.activityLogService = activityLogService;
    }

    // --- Upload court support materials ---
    @Transactional
    public List<CourtSupportMaterial> uploadMaterials(
            UUID clientUuid,
            UUID caseUuid,
            List<Map<String, Object>> descriptions,
            List<MultipartFile> files
    ) throws IOException {

        List<CourtSupportMaterial> savedMaterials = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            Map<String, Object> description = descriptions.get(i);
            MultipartFile file = files.get(i);

            // Prevent duplicate file names for same client
            if (repository.existsByClientUuidAndFileName(clientUuid, file.getOriginalFilename())) {
                throw new RuntimeException("File with name " + file.getOriginalFilename() + " already exists");
            }

            com.legalpro.accountservice.util.UploadValidation.validate(
                    file, com.legalpro.accountservice.util.UploadValidation.DOCUMENT_CONTENT_TYPES);
            String safeFileName = com.legalpro.accountservice.util.UploadValidation.sanitizeFileName(
                    file.getOriginalFilename());

            // Generate unique filename for GCS
            String objectName = clientUuid + "/" + UUID.randomUUID() + "-" + safeFileName;

            // Upload to GCS
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType(file.getContentType())
                    .setCacheControl("public, max-age=31536000, immutable")
                    .build();
            storage.create(blobInfo, file.getBytes());

            // Save metadata in DB
            CourtSupportMaterial material = CourtSupportMaterial.builder()
                    .clientUuid(clientUuid)
                    .caseUuid(caseUuid)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileUrl("gs://" + bucketName + "/" + objectName)
                    .description(description) // ✅ JSON object
                    .createdAt(LocalDateTime.now())
                    .build();

            savedMaterials.add(repository.save(material));
        }

        return savedMaterials;
    }

    // --- Get materials for client ---
    public List<CourtSupportMaterial> getMaterials(UUID clientUuid) {
        List<CourtSupportMaterial> materials = repository.findAllByClientUuidAndDeletedAtIsNull(clientUuid);

        // These entities are already detached by this point (no active
        // transaction spans this method), so setting a signed URL here is
        // safe -- it will not be flushed back to the database. Signed in
        // parallel since each signing call is a real network round-trip.
        materials.parallelStream().forEach(material ->
                material.setFileUrl(com.legalpro.accountservice.util.GcsUrlSigner.sign(material.getFileUrl())));

        return materials;
    }

    // --- Get single material by ID ---
    public Optional<CourtSupportMaterial> getMaterial(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    // --- Soft delete material ---
    public void softDeleteMaterial(Long id) {
        repository.deleteById(id);
    }

    // --- Upload a single file with description ---
    @Transactional
    public CourtSupportMaterial uploadMaterial(UUID clientUuid, UUID caseUuid, Map<String, Object> description, MultipartFile file) throws IOException {

        // Optional: check if client already has file with same name
        if (repository.existsByClientUuidAndFileName(clientUuid, file.getOriginalFilename())) {
            throw new RuntimeException("Client already has a file with name: " + file.getOriginalFilename());
        }

        com.legalpro.accountservice.util.UploadValidation.validate(
                file, com.legalpro.accountservice.util.UploadValidation.DOCUMENT_CONTENT_TYPES);
        String safeFileName = com.legalpro.accountservice.util.UploadValidation.sanitizeFileName(
                file.getOriginalFilename());

        // Generate unique GCS object name
        String objectName = clientUuid + "/" + UUID.randomUUID() + "-" + safeFileName;

        // Upload file to GCS
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(file.getContentType())
                .setCacheControl("public, max-age=31536000, immutable")
                .build();
        storage.create(blobInfo, file.getBytes());

        // Save metadata in DB
        CourtSupportMaterial material = CourtSupportMaterial.builder()
                .uuid(UUID.randomUUID())
                .clientUuid(clientUuid)
                .caseUuid(caseUuid)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileUrl("gs://" + bucketName + "/" + objectName)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        material = repository.save(material);

        // Fetch lawyerUuid using caseUuid (if case present)
        UUID lawyerUuid = null;

        if (caseUuid != null) {
            LegalCase caseEntity = legalCaseRepository.findByUuid(caseUuid)
                    .orElseThrow(() -> new RuntimeException("Case not found"));
            lawyerUuid = caseEntity.getLawyerUuid();
        }

        activityLogService.logActivity(
                "DOCUMENT_UPLOADED",
                material.getFileName() + " uploaded",
                clientUuid,                // actorUuid (client)
                lawyerUuid,                // lawyerUuid
                clientUuid,                // clientUuid
                caseUuid,                  // caseUuid
                material.getUuid(),        // referenceUuid
                null                       // metadata
        );

        return material;
    }

    public List<CourtSupportMaterial> getMaterialsByCase(UUID clientUuid, UUID caseUuid) {
        List<CourtSupportMaterial> materials = repository.findAllByClientUuidAndCaseUuidAndDeletedAtIsNull(clientUuid, caseUuid);

        // Already-detached entities here too -- safe to mutate for the response.
        materials.parallelStream().forEach(material ->
                material.setFileUrl(com.legalpro.accountservice.util.GcsUrlSigner.sign(material.getFileUrl())));

        return materials;
    }
}
