package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfilePictureService {

    private final AccountRepository accountRepository;
    private final Storage storage;

    private final String bucketName = "legalpro-client-docs-au";
    private static final String PUBLIC_BASE = "https://storage.googleapis.com";

    @Transactional
    public String uploadProfilePicture(UUID userUuid, MultipartFile file) throws IOException {

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/png") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/webp"))) {

            throw new RuntimeException("Invalid file type. Allowed: png, jpg, jpeg, webp");
        }

        Account account = accountRepository.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Generate path
        String extension = Objects.requireNonNull(file.getOriginalFilename())
                .substring(file.getOriginalFilename().lastIndexOf('.') + 1);

        String objectName = "profile-pictures/" + userUuid + "/" + UUID.randomUUID() + "." + extension;

        // Upload to GCS
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(contentType)
                .setCacheControl("public, max-age=31536000, immutable")
                .build();

        storage.create(blobInfo, file.getBytes());

        // The bucket is private -- store the gs:// reference (not a public
        // URL) and hand back a short-lived signed URL for immediate display.
        // Every other read path already converts this reference to a fresh
        // signed URL via GcsUrlSigner at response time.
        String gsUrl = "gs://" + bucketName + "/" + objectName;

        account.setProfilePictureUrl(gsUrl);
        accountRepository.save(account);

        return com.legalpro.accountservice.util.GcsUrlSigner.sign(gsUrl);
    }
}

