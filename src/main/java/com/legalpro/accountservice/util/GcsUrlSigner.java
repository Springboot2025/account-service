package com.legalpro.accountservice.util;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.util.concurrent.ConcurrentHashMap;
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

    // Signing is a real network round-trip to Google's IAM API (roughly
    // 100-300ms) on every call. Caching for most of the signed URL's actual
    // lifetime means repeat views of the same document within that window
    // are free, and lists of many documents only pay the signing cost once
    // per distinct file instead of once per request.
    private static final long CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(12);

    // legalpro-profile-pictures-au is a fully public bucket (avatars, not
    // sensitive documents) -- references pointing at it never need signing.
    private static final String PUBLIC_BUCKET = "legalpro-profile-pictures-au";

    private static final Pattern HTTPS_PATTERN =
            Pattern.compile("^https://storage\\.googleapis\\.com/([^/]+)/(.+)$");

    private static volatile Storage storage;

    private static final ConcurrentHashMap<String, CachedSignedUrl> CACHE = new ConcurrentHashMap<>();

    private record ParsedReference(String bucket, String object) {
    }

    private record CachedSignedUrl(String url, long expiresAtMillis) {
    }

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
     * some other external URL). References pointing at the public
     * profile-pictures bucket are returned as-is (no signing needed).
     */
    public static String sign(String storedReference) {
        ParsedReference parsed = parse(storedReference);
        if (parsed == null) {
            return storedReference;
        }
        if (parsed.bucket().equals(PUBLIC_BUCKET)) {
            return publicUrl(parsed);
        }

        String cached = cachedOrNull(storedReference);
        if (cached != null) {
            return cached;
        }
        return signFreshAndCache(storedReference, parsed);
    }

    /**
     * Batch variant of {@link #sign(String)} for a list of references (e.g.
     * a case's documents). Cache hits and public-bucket references resolve
     * immediately; anything that actually needs a fresh signBlob call is
     * fanned out in parallel instead of signed one at a time, so a list of
     * N files costs roughly the time of the single slowest call instead of
     * N calls added up.
     */
    public static java.util.List<String> signAll(java.util.List<String> storedReferences) {
        if (storedReferences == null) {
            return null;
        }

        int n = storedReferences.size();
        String[] results = new String[n];
        java.util.List<Integer> pendingIndexes = new java.util.ArrayList<>();

        for (int i = 0; i < n; i++) {
            String ref = storedReferences.get(i);
            ParsedReference parsed = parse(ref);
            if (parsed == null) {
                results[i] = ref;
            } else if (parsed.bucket().equals(PUBLIC_BUCKET)) {
                results[i] = publicUrl(parsed);
            } else {
                String cached = cachedOrNull(ref);
                if (cached != null) {
                    results[i] = cached;
                } else {
                    pendingIndexes.add(i);
                }
            }
        }

        if (!pendingIndexes.isEmpty()) {
            java.util.List<Thread> threads = new java.util.ArrayList<>(pendingIndexes.size());
            for (int idx : pendingIndexes) {
                String ref = storedReferences.get(idx);
                ParsedReference parsed = parse(ref);
                threads.add(Thread.ofVirtual().start(() -> results[idx] = signFreshAndCache(ref, parsed)));
            }
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return java.util.Arrays.asList(results);
    }

    private static String cachedOrNull(String storedReference) {
        CachedSignedUrl cached = CACHE.get(storedReference);
        if (cached == null) {
            return null;
        }
        if (cached.expiresAtMillis() <= System.currentTimeMillis()) {
            CACHE.remove(storedReference, cached);
            return null;
        }
        return cached.url();
    }

    private static String signFreshAndCache(String storedReference, ParsedReference parsed) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(parsed.bucket(), parsed.object()).build();
            String signed = storage()
                    .signUrl(blobInfo, EXPIRY_MINUTES, TimeUnit.MINUTES,
                            Storage.SignUrlOption.withV4Signature())
                    .toString();
            CACHE.put(storedReference, new CachedSignedUrl(signed, System.currentTimeMillis() + CACHE_TTL_MILLIS));
            return signed;
        } catch (Exception e) {
            // A dead link is a better failure mode than a stack trace bubbling
            // up through a DTO-mapping call site -- log and return null so the
            // frontend just shows nothing for this one image/document instead
            // of the whole response failing.
            System.err.println("Failed to sign GCS URL for " + storedReference + ": " + e.getMessage());
            return null;
        }
    }

    private static String publicUrl(ParsedReference parsed) {
        return "https://storage.googleapis.com/" + parsed.bucket() + "/" + parsed.object();
    }

    private static ParsedReference parse(String storedReference) {
        if (storedReference == null || storedReference.isBlank()) {
            return null;
        }

        if (storedReference.startsWith("gs://")) {
            String withoutScheme = storedReference.substring("gs://".length());
            int slash = withoutScheme.indexOf('/');
            if (slash < 0) {
                return null; // malformed, nothing to sign
            }
            return new ParsedReference(withoutScheme.substring(0, slash), withoutScheme.substring(slash + 1));
        }

        Matcher matcher = HTTPS_PATTERN.matcher(storedReference);
        if (!matcher.matches()) {
            return null; // not a reference we recognize -- leave it alone
        }
        return new ParsedReference(matcher.group(1), matcher.group(2));
    }

    /**
     * Drops expired cache entries. Called periodically (see
     * TokenSessionCleanupJob) so the cache doesn't grow unbounded over the
     * life of a long-running instance.
     */
    public static void evictExpiredCache() {
        long now = System.currentTimeMillis();
        CACHE.entrySet().removeIf(e -> e.getValue().expiresAtMillis() <= now);
    }
}
