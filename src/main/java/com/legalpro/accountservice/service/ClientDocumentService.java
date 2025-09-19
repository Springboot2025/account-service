package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.ClientDocument;
import com.legalpro.accountservice.repository.ClientDocumentRepository;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientDocumentService {

    private final ClientDocumentRepository repository;
    private final Storage storage;
    private final String bucketName = "legalpro-client-docs"; // GCS bucket name

    public ClientDocumentService(ClientDocumentRepository repository) {
        this.repository = repository;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    public ClientDocument uploadDocument(UUID clientUuid, UUID lawyerUuid, MultipartFile file) throws IOException {
        // Generate unique filename
        String objectName = clientUuid + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        // Upload to GCS
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        // Save metadata in DB
        ClientDocument document = ClientDocument.builder()
                .clientUuid(clientUuid)
                .lawyerUuid(lawyerUuid) // may be null
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileUrl("gs://" + bucketName + "/" + objectName)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(document);
    }


    // --- Query methods ---

    public List<ClientDocument> getClientDocuments(UUID clientUuid) {
        return repository.findAllByClientUuidAndDeletedAtIsNull(clientUuid);
    }

    public List<ClientDocument> getClientDocumentsForLawyer(UUID clientUuid, UUID lawyerUuid) {
        return repository.findAllByClientUuidAndLawyerUuidAndDeletedAtIsNull(clientUuid, lawyerUuid);
    }

    public Optional<ClientDocument> getDocument(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    public void softDeleteDocument(Long id) {
        repository.findByIdAndDeletedAtIsNull(id).ifPresent(doc -> {
            doc.setDeletedAt(LocalDateTime.now());
            repository.save(doc);
        });
    }
}
