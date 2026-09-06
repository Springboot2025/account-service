package com.legalpro.accountservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Keeps the file at ANTHROPIC_IDENTITY_TOKEN_FILE populated with a current
 * Google-signed identity token, fetched from the Cloud Run/GCE/GKE instance
 * metadata server.
 *
 * The Anthropic SDK's Workload Identity Federation credential resolver
 * (wired up in AnthropicConfig via AnthropicOkHttpClient.builder().fromEnv())
 * re-reads this file's contents fresh every time it needs to exchange for a
 * new Claude access token — there is no in-memory caching on its side for
 * the identity token itself. This class is what actually keeps that file
 * current; without it, the token would go stale (Google identity tokens
 * expire after roughly an hour) and every exchange after that would fail.
 *
 * A no-op everywhere ANTHROPIC_IDENTITY_TOKEN_FILE isn't set (e.g. local
 * dev, or a deployment still using a static ANTHROPIC_API_KEY) — it never
 * attempts to reach the metadata server unless WIF is actually configured,
 * and metadata-server calls only succeed when actually running on GCP anyway.
 */
@Component
public class AnthropicIdentityTokenRefresher {

    private static final String GCP_METADATA_IDENTITY_URL =
            "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity"
                    + "?audience=https://api.anthropic.com&format=full";

    private final HttpClient metadataHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @PostConstruct
    public void refreshOnStartup() {
        refresh();
    }

    // Google identity tokens are valid for roughly an hour; refreshing every
    // 20 minutes leaves a comfortable margin even if a single refresh fails.
    @Scheduled(fixedRate = 20 * 60 * 1000L)
    public void refresh() {
        String tokenFilePath = System.getenv("ANTHROPIC_IDENTITY_TOKEN_FILE");
        if (tokenFilePath == null || tokenFilePath.isBlank()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GCP_METADATA_IDENTITY_URL))
                    .header("Metadata-Flavor", "Google")
                    .GET()
                    .build();
            HttpResponse<String> response =
                    metadataHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("WARNING: Failed to refresh the Anthropic WIF identity token — "
                        + "metadata server returned " + response.statusCode());
                return;
            }

            Path path = Paths.get(tokenFilePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, response.body().trim());
        } catch (Exception e) {
            System.err.println("WARNING: Failed to refresh the Anthropic WIF identity token from the "
                    + "Cloud Run metadata server (expected when running outside GCP): " + e.getMessage());
        }
    }
}
