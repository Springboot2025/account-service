package com.legalpro.accountservice.util;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a stored GCS reference (either "gs://bucket/object" or a legacy
 * "https://storage.googleapis.com/bucket/object" public URL from before the
 * buckets were made private) into a short-lived V4 signed URL.
 *
 * The storage buckets used to grant allUsers:objectViewer -- anyone with a
 * link could read a client's passport, driver's licence, or court evidence
 * forever, with no expiry and no way to revoke access. They are now private;
 * this is the only supported way to actually view a file, and it only ever
 * gets called from code paths that have already checked the requester is
 * authorized to see that specific resource.
 *
 * Signing works without a downloaded service account key: on Cloud Run the
 * attached service account signs via the IAM Credentials API (signBlob),
 * which requires that service account to hold roles/iam.serviceAccountTokenCreator
 * on itself.
 */
public final class GcsUrlSigner {

    private static final long EXPIRY_MINUTES = 15;

    private static final Pattern HTTPS_PATTERN =
            Pattern.compile("^https://storage\\.googleapis\\.com/([^/]+)/(.+)$");

    private static volatile Storage storage;

    private GcsUrlSigner() {
    }

    private static Storage storage() {
        Storage local = storage;
        if (local == null) {
            synchronized (GcsUrlSigner.class) {
                local = storage;
                if (local == null) {
                    local = StorageOptions.getDefaultInstance().getService();
                    storage = local;
                }
            }
        }
        return local;
    }

    /**
     * Returns a signed, time-limited URL for the given stored reference, or
     * the input unchanged if it's null/blank or doesn't look like a
     * reference this class knows how to sign (e.g. already a signed URL, or
     * some other external URL).
     */
    public static String sign(String storedReference) {
        if (storedReference == null || storedReference.isBlank()) {
            return storedReference;
        }

        String bucket;
        String object;

        if (storedReference.startsWith("gs://")) {
            String withoutScheme = storedReference.substring("gs://".length());
            int slash = withoutScheme.indexOf('/');
            if (slash < 0) {
                return storedReference; // malformed, nothing to sign
            }
            bucket = withoutScheme.substring(0, slash);
            object = withoutScheme.substring(slash + 1);
        } else {
            Matcher matcher = HTTPS_PATTERN.matcher(storedReference);
            if (!matcher.matches()) {
                return storedReference; // not a reference we recognize -- leave it alone
            }
            bucket = matcher.group(1);
            object = matcher.group(2);
        }

        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, object).build();
            return storage()
                    .signUrl(blobInfo, EXPIRY_MINUTES, TimeUnit.MINUTES,
                            Storage.SignUrlOption.withV4Signature())
                    .toString();
        } catch (Exception e) {
            // A dead link is a better failure mode than a stack trace bubbling
            // up through a DTO-mapping call site -- log and return null so the
            // frontend just shows nothing for this one image/document instead
            // of the whole response failing.
            System.err.println("Failed to sign GCS URL for " + storedReference + ": " + e.getMessage());
            return null;
        }
    }
}
