package com.legalpro.accountservice.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the Anthropic client using Workload Identity Federation (WIF) —
 * there is no static API key anywhere in this application. This replaces the
 * previous ANTHROPIC_API_KEY approach after a static key leaked into a
 * frontend build and ran up unauthorized charges.
 *
 * {@code fromEnv()} triggers the SDK's built-in credential-resolution chain,
 * which reads (in order of precedence): ANTHROPIC_API_KEY / ANTHROPIC_AUTH_TOKEN
 * (static, not used here — must stay unset), then workload identity federation
 * from ANTHROPIC_FEDERATION_RULE_ID + ANTHROPIC_ORGANIZATION_ID +
 * ANTHROPIC_SERVICE_ACCOUNT_ID + ANTHROPIC_IDENTITY_TOKEN_FILE.
 *
 * ANTHROPIC_IDENTITY_TOKEN_FILE points at a file whose contents the SDK
 * re-reads fresh on every token exchange (see AnthropicIdentityTokenRefresher,
 * which keeps that file populated with a current Google-signed identity
 * token fetched from the Cloud Run metadata server).
 *
 * If none of these are set (e.g. local dev), {@code build()} still succeeds —
 * the resolver swallows the "no credentials" case and simply builds a client
 * with no credentials applied. The rest of the app must keep working;
 * LetterOfAdviceAiServiceImpl surfaces a clear error only when an actual
 * generation request is attempted without a working credential.
 *
 * Setup required outside this code (Anthropic's WIF docs for Google Cloud):
 * 1. Attach a DEDICATED service account to this Cloud Run service — not the
 *    default Compute Engine service account.
 * 2. In Claude Console → Settings → Workload identity → Connect workload →
 *    Google Cloud, run the wizard against that service account's email +
 *    unique ID. It creates a federation rule (fdrl_...), an Anthropic
 *    service account (svac_...), and registers the issuer.
 * 3. Set ANTHROPIC_FEDERATION_RULE_ID, ANTHROPIC_ORGANIZATION_ID,
 *    ANTHROPIC_SERVICE_ACCOUNT_ID, and ANTHROPIC_IDENTITY_TOKEN_FILE (e.g.
 *    /tmp/anthropic-identity-token.jwt) as plain environment variables — not
 *    secrets, they're identifiers and a file path, not credentials.
 * 4. Confirm ANTHROPIC_API_KEY is completely unset anywhere this service
 *    runs — if present, it takes precedence over WIF and none of this does
 *    anything.
 */
@Configuration
public class AnthropicConfig {

    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.builder().fromEnv().build();
    }
}
