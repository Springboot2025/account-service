package com.legalpro.accountservice.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared file-upload validation: content-type allowlist, size cap, and
 * filename sanitization for the GCS object key. Every upload endpoint used
 * to duplicate (or skip) this logic independently -- several accepted any
 * file type/size and wrote the user-supplied filename directly into the
 * storage path unsanitized.
 */
public final class UploadValidation {

    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024; // 15MB

    public static final Set<String> DOCUMENT_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png", "image/jpeg", "image/jpg", "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
    );

    public static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp"
    );

    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[^A-Za-z0-9._-]");

    private UploadValidation() {
    }

    /**
     * @throws IllegalArgumentException if the file's content type isn't in
     *      {@code allowedContentTypes}, or it exceeds {@code maxSizeBytes}.
     */
    public static void validate(MultipartFile file, Set<String> allowedContentTypes, long maxSizeBytes) {
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType + ". Allowed: " + allowedContentTypes);
        }
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                    "File exceeds the " + (maxSizeBytes / (1024 * 1024)) + "MB size limit: "
                            + file.getOriginalFilename());
        }
    }

    public static void validate(MultipartFile file, Set<String> allowedContentTypes) {
        validate(file, allowedContentTypes, DEFAULT_MAX_FILE_SIZE_BYTES);
    }

    /**
     * Strips any directory component and any character outside a safe
     * allowlist, so a filename can never smuggle a path traversal or
     * unexpected structure into the GCS object key.
     */
    public static String sanitizeFileName(String originalFilename) {
        String base = originalFilename == null ? "file" : originalFilename;
        base = base.replace("\\", "/");
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash >= 0) {
            base = base.substring(lastSlash + 1);
        }
        String sanitized = UNSAFE_FILENAME_CHARS.matcher(base).replaceAll("_");
        return sanitized.isBlank() ? "file" : sanitized;
    }
}
