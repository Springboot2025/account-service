package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.ClientDocument;
import com.legalpro.accountservice.repository.ClientDocumentRepository;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientDocumentService {

    private final ClientDocumentRepository repository;
    private final Storage storage;
    private final String bucketName = "legalpro-client-docs"; // GCS bucket name
    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com";

    public ClientDocumentService(ClientDocumentRepository repository) {
        this.repository = repository;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    // --- Upload document with type ---
    @Transactional
    public List<ClientDocument> uploadDocuments(UUID clientUuid, UUID lawyerUuid, UUID caseUuid, List<String> documentTypes, List<MultipartFile> files) throws IOException {
        List<ClientDocument> savedDocuments = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            String documentType = documentTypes.get(i);
            MultipartFile file = files.get(i);

            // Check if client already has a document of this type
            if (existsByClientAndDocumentType(clientUuid, documentType)) {
                throw new RuntimeException("Client already has a document of type: " + documentType);
            }

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
                    .lawyerUuid(lawyerUuid)
                    .caseUuid(caseUuid)
                    .documentType(documentType)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileUrl("gs://" + bucketName + "/" + objectName)
                    .createdAt(LocalDateTime.now())
                    .build();

            savedDocuments.add(repository.save(document));
        }

        return savedDocuments;
    }


    // --- Check if client already has document type ---
    public boolean existsByClientAndDocumentType(UUID clientUuid, String documentType) {
        return repository.existsByClientUuidAndDocumentType(clientUuid, documentType);
    }

    // --- Query methods ---
    public List<ClientDocument> getClientDocuments(UUID clientUuid) {
        List<ClientDocument> docs = repository.findAllByClientUuidAndDeletedAtIsNull(clientUuid);

        for (ClientDocument doc : docs) {
            String fileUrl = doc.getFileUrl();
            if (fileUrl != null && fileUrl.startsWith("gs://")) {
                String withoutScheme = fileUrl.substring("gs://".length()); 
                doc.setFileUrl(GCS_PUBLIC_BASE + "/" + withoutScheme);
            }
        }

        return docs;
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

    public List<ClientDocument> getClientDocumentsByType(UUID clientUuid, String documentType) {
        return repository.findByClientUuidAndDocumentType(clientUuid, documentType);
    }

}
