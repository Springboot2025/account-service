package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Resend-backed EmailService. Only created when EMAIL_PROVIDER=resend; when it
 * is, it is @Primary so every existing EmailService injection point picks it
 * up while the SendGrid implementation stays in place, unchanged. With the
 * property unset, this bean doesn't exist and SendGrid remains the only one.
 */
@Service("resendEmailService")
@Primary
@ConditionalOnProperty(name = "email.provider", havingValue = "resend")
public class ResendEmailServiceImpl implements EmailService {

    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");

    private final String apiKey;
    private final String from;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResendEmailServiceImpl(
            @Value("${RESEND_API_KEY:}") String apiKey,
            // Resend only sends from a verified domain (or its
            // onboarding@resend.dev test address, which can only deliver to the
            // account owner's own email).
            @Value("${RESEND_FROM_EMAIL:onboarding@resend.dev}") String fromEmail,
            // Shown as the sender name in the recipient's inbox instead of
            // the bare address (e.g. "Boss Justice" rather than "noreply").
            @Value("${RESEND_FROM_NAME:Boss Justice}") String fromName
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("EMAIL_PROVIDER=resend but RESEND_API_KEY is not set");
        }
        this.apiKey = apiKey;
        this.from = (fromName == null || fromName.isBlank())
                ? fromEmail
                : fromName + " <" + fromEmail + ">";
    }

    @Override
    public void sendEmail(String to, String subject, String bodyHtml) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "from", from,
                    "to", List.of(to),
                    "subject", subject,
                    "html", bodyHtml
            ));

            HttpRequest request = HttpRequest.newBuilder(RESEND_ENDPOINT)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException(
                        "Resend API error: status=" + response.statusCode() + ", body=" + response.body());
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to send email via Resend", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending email via Resend", ex);
        }
    }
}
