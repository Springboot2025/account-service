package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.CourtSupportMaterial;
import com.legalpro.accountservice.repository.CourtSupportMaterialRepository;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CourtSupportMaterialService {

    private final CourtSupportMaterialRepository repository;
    private final Storage storage;
    private final String bucketName = "legalpro-client-docs"; // ✅ dedicated bucket
    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com";

    public CourtSupportMaterialService(CourtSupportMaterialRepository repository) {
        this.repository = repository;
        this.storage = StorageOptions.getDefaultInstance().getService();
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

            // Generate unique filename for GCS
            String objectName = clientUuid + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

            // Upload to GCS
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType(file.getContentType())
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

        // Convert gs:// URLs to https:// URLs
        for (CourtSupportMaterial material : materials) {
            String fileUrl = material.getFileUrl();
            if (fileUrl != null && fileUrl.startsWith("gs://")) {
                String withoutScheme = fileUrl.substring("gs://".length());
                material.setFileUrl(GCS_PUBLIC_BASE + "/" + withoutScheme);
            }
        }

        return materials;
    }

    // --- Get single material by ID ---
    public Optional<CourtSupportMaterial> getMaterial(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    // --- Soft delete material ---
    public void softDeleteMaterial(Long id) {
        repository.findByIdAndDeletedAtIsNull(id).ifPresent(material -> {
            material.setDeletedAt(LocalDateTime.now());
            repository.save(material);
        });
    }

    // --- Upload a single file with description ---
    @Transactional
    public CourtSupportMaterial uploadMaterial(UUID clientUuid, UUID caseUuid, Map<String, Object> description, MultipartFile file) throws IOException {

        // Optional: check if client already has file with same name
        if (repository.existsByClientUuidAndFileName(clientUuid, file.getOriginalFilename())) {
            throw new RuntimeException("Client already has a file with name: " + file.getOriginalFilename());
        }

        // Generate unique GCS object name
        String objectName = clientUuid + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        // Upload file to GCS
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());

        // Save metadata in DB
        CourtSupportMaterial material = CourtSupportMaterial.builder()
                .clientUuid(clientUuid)
                .caseUuid(caseUuid)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileUrl("gs://" + bucketName + "/" + objectName)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(material);
    }

}
