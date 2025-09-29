package com.legalpro.accountservice.util;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.SignUrlOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.concurrent.TimeUnit;

@Component
public class GcsSignedUrlUtil {

    private final Storage storage;

    // duration in minutes (configurable)
    private final long durationMinutes;

    public GcsSignedUrlUtil(@Value("${gcs.signed-url.duration-minutes:15}") long durationMinutes) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.durationMinutes = durationMinutes;
    }

    public String generateSignedUrl(String bucketName, String objectName) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();
        URL signedUrl = storage.signUrl(
                blobInfo,
                durationMinutes,
                TimeUnit.MINUTES,
                SignUrlOption.withV4Signature()
        );
        return signedUrl.toString();
    }
}
