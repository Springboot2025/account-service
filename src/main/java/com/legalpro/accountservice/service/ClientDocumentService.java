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
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientDocumentService {

    private final ClientDocumentRepository repository;
    private final Storage storage;
    private final GcsSignedUrlUtil signedUrlUtil;
    private final String bucketName = "legalpro-client-docs"; // GCS bucket name

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

            // Save metadata in DB (store gs:// path)
            ClientDocument document = ClientDocument.builder()
                    .clientUuid(clientUuid)
                    .lawyerUuid(lawyerUuid)
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

        // Generate signed URL for each document
        for (ClientDocument doc : docs) {
            String gcsPath = doc.getFileUrl(); // e.g., gs://bucket/object
            if (gcsPath != null && gcsPath.startsWith("gs://")) {
                String bucket = gcsPath.split("/", 4)[2];
                String object = gcsPath.split("/", 4)[3];

                BlobInfo blobInfo = BlobInfo.newBuilder(bucket, object).build();

                URL signedUrl = storage.signUrl(
                        blobInfo,
                        15, // expires in 15 minutes
                        java.util.concurrent.TimeUnit.MINUTES,
                        Storage.SignUrlOption.withV4Signature()
                );

                doc.setFileUrl(signedUrl.toString()); // replace gs:// link with signed URL
            }
        }

        return docs;
    }

    public List<ClientDocument> getClientDocumentsForLawyer(UUID clientUuid, UUID lawyerUuid) {
        List<ClientDocument> docs = repository.findAllByClientUuidAndLawyerUuidAndDeletedAtIsNull(clientUuid, lawyerUuid);

        for (ClientDocument doc : docs) {
            if (doc.getFileUrl() != null && doc.getFileUrl().startsWith("gs://")) {
                String objectPath = extractObjectPath(doc.getFileUrl(), bucketName);
                try {
                    String signed = signedUrlUtil.generateSignedUrl(bucketName, objectPath);
                    doc.setFileUrl(signed);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return docs;
    }

    public Optional<ClientDocument> getDocument(Long id) {
        Optional<ClientDocument> opt = repository.findByIdAndDeletedAtIsNull(id);
        opt.ifPresent(doc -> {
            if (doc.getFileUrl() != null && doc.getFileUrl().startsWith("gs://")) {
                String objectPath = extractObjectPath(doc.getFileUrl(), bucketName);
                try {
                    String signed = signedUrlUtil.generateSignedUrl(bucketName, objectPath);
                    doc.setFileUrl(signed);
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

    private String extractObjectPath(String gsUrl, String bucket) {
        // gs://bucketName/object/path
        String prefix = "gs://" + bucket + "/";
        if (gsUrl.startsWith(prefix)) {
            return gsUrl.substring(prefix.length());
        }
        // fallback: remove gs:// and first segment
        return gsUrl.replaceFirst("^gs://[^/]+/", "");
    }
}
