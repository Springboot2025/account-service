package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.ClientDocument;
import com.legalpro.accountservice.repository.ClientDocumentRepository;
import com.legalpro.accountservice.util.GcsSignedUrlUtil;
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
    private final GcsSignedUrlUtil signedUrlUtil;
    private final Storage storage;
    private final String bucketName = "legalpro-client-docs";

    public ClientDocumentService(ClientDocumentRepository repository,
                                 GcsSignedUrlUtil signedUrlUtil) {
        this.repository = repository;
        this.signedUrlUtil = signedUrlUtil;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    // --- Upload document with type ---
    @Transactional
    public List<ClientDocument> uploadDocuments(UUID clientUuid, UUID lawyerUuid, List<String> documentTypes, List<MultipartFile> files) throws IOException {
        List<ClientDocument> savedDocuments = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            String documentType = documentTypes.get(i);
            MultipartFile file = files.get(i);

            if (existsByClientAndDocumentType(clientUuid, documentType)) {
                throw new RuntimeException("Client already has a document of type: " + documentType);
            }

            String objectName = clientUuid + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            ClientDocument document = ClientDocument.builder()
                    .clientUuid(clientUuid)
                    .lawyerUuid(lawyerUuid)
                    .documentType(documentType)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileUrl("gs://" + bucketName + "/" + objectName) // store gs:// path
                    .createdAt(LocalDateTime.now())
                    .build();

            savedDocuments.add(repository.save(document));
        }

        return savedDocuments;
    }

    // --- Check if document exists ---
    public boolean existsByClientAndDocumentType(UUID clientUuid, String documentType) {
        return repository.existsByClientUuidAndDocumentType(clientUuid, documentType);
    }

    // --- Get documents for client (signed URLs) ---
    public List<ClientDocument> getClientDocuments(UUID clientUuid) {
        List<ClientDocument> docs = repository.findAllByClientUuidAndDeletedAtIsNull(clientUuid);

        docs.forEach(doc -> {
            if (doc.getFileUrl() != null && doc.getFileUrl().startsWith("gs://")) {
                try {
                    String objectPath = extractObjectPath(doc.getFileUrl(), bucketName);
                    String signedUrl = signedUrlUtil.generateSignedUrl(bucketName, objectPath);
                    doc.setFileUrl(signedUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return docs;
    }

    // --- Get documents for lawyer (signed URLs) ---
    public List<ClientDocument> getClientDocumentsForLawyer(UUID clientUuid, UUID lawyerUuid) {
        List<ClientDocument> docs = repository.findAllByClientUuidAndLawyerUuidAndDeletedAtIsNull(clientUuid, lawyerUuid);

        docs.forEach(doc -> {
            if (doc.getFileUrl() != null && doc.getFileUrl().startsWith("gs://")) {
                try {
                    String objectPath = extractObjectPath(doc.getFileUrl(), bucketName);
                    String signedUrl = signedUrlUtil.generateSignedUrl(bucketName, objectPath);
                    doc.setFileUrl(signedUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return docs;
    }

    // --- Get single document (signed URL) ---
    public Optional<ClientDocument> getDocument(Long id) {
        Optional<ClientDocument> opt = repository.findByIdAndDeletedAtIsNull(id);

        opt.ifPresent(doc -> {
            if (doc.getFileUrl() != null && doc.getFileUrl().startsWith("gs://")) {
                try {
                    String objectPath = extractObjectPath(doc.getFileUrl(), bucketName);
                    String signedUrl = signedUrlUtil.generateSignedUrl(bucketName, objectPath);
                    doc.setFileUrl(signedUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return opt;
    }

    public void softDeleteDocument(Long id) {
        repository.findByIdAndDeletedAtIsNull(id).ifPresent(doc -> {
            doc.setDeletedAt(LocalDateTime.now());
            repository.save(doc);
        });
    }

    // --- Helper to get object path from gs:// URL ---
    private String extractObjectPath(String gsUrl, String bucket) {
        String prefix = "gs://" + bucket + "/";
        if (gsUrl.startsWith(prefix)) {
            return gsUrl.substring(prefix.length());
        }
        return gsUrl.replaceFirst("^gs://[^/]+/", "");
    }
}
