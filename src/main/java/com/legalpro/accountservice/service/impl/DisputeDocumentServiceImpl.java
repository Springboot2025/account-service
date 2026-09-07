package com.legalpro.accountservice.service.impl;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.legalpro.accountservice.entity.DisputeDocument;
import com.legalpro.accountservice.repository.DisputeDocumentRepository;
import com.legalpro.accountservice.service.DisputeDocumentService;
import com.legalpro.accountservice.util.UploadValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeDocumentServiceImpl implements DisputeDocumentService {

    private final DisputeDocumentRepository disputeDocumentRepository;
    private final Storage storage = StorageOptions.getDefaultInstance().getService();
    private final String bucketName = "legalpro-dispute-docs-au";

    // This endpoint is intentionally reachable without authentication (a
    // public dispute-intake form), which makes content validation the only
    // real defense -- previously any file type/size was accepted with the
    // user-supplied filename written directly into the storage path.
    @Override
    public void uploadDocuments(UUID disputeUuid, List<MultipartFile> files) throws IOException {

        for (MultipartFile file : files) {
            UploadValidation.validate(file, UploadValidation.DOCUMENT_CONTENT_TYPES);
            String safeFileName = UploadValidation.sanitizeFileName(file.getOriginalFilename());

            String objectName = "disputes/" + disputeUuid + "/" + UUID.randomUUID() + "-" + safeFileName;

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType(file.getContentType())
                    .setCacheControl("public, max-age=31536000, immutable")
                    .build();

            storage.create(blobInfo, file.getBytes());

            DisputeDocument document = DisputeDocument.builder()
                    .disputeUuid(disputeUuid)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileUrl("gs://" + bucketName + "/" + objectName)
                    .createdAt(LocalDateTime.now())
                    .build();

            disputeDocumentRepository.save(document);
        }
    }
}
