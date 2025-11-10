package com.legalpro.accountservice.service.impl;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.legalpro.accountservice.entity.DisputeDocument;
import com.legalpro.accountservice.repository.DisputeDocumentRepository;
import com.legalpro.accountservice.service.DisputeDocumentService;
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
    private final String bucketName = "legalpro-dispute-docs"; // create or reuse bucket

    @Override
    public void uploadDocuments(UUID disputeUuid, List<MultipartFile> files) throws IOException {

        for (MultipartFile file : files) {

            String objectName = "disputes/" + disputeUuid + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType(file.getContentType())
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
