package com.legalpro.accountservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequest(wrappedRequest);
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse(); // Important to actually send response
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        StringBuilder msg = new StringBuilder();

        msg.append("\n--- Incoming Request ---\n");
        msg.append(request.getMethod()).append(" ").append(request.getRequestURI()).append("\n");

        Collections.list(request.getHeaderNames())
                .forEach(header -> msg.append(header).append(": ").append(request.getHeader(header)).append("\n"));

        String contentType = request.getContentType();
        if (contentType == null) contentType = "";

        // Only log textual bodies
        if (contentType.startsWith("application/json") || contentType.startsWith("text/")) {
            byte[] buf = request.getContentAsByteArray();
            if (buf.length > 0) {
                String body = new String(buf, StandardCharsets.UTF_8);
                msg.append("Body: ").append(body).append("\n");
            }
        } else if (contentType.contains("multipart/form-data")) {
            msg.append("Multipart request: skipping raw body logging\n");
        } else if (!contentType.isEmpty()) {
            msg.append("Non-text request body skipped: Content-Type=").append(contentType).append("\n");
        }

        System.out.println(msg);
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        StringBuilder msg = new StringBuilder();

        msg.append("\n--- Outgoing Response ---\n");
        msg.append("Status: ").append(response.getStatus()).append("\n");

        response.getHeaderNames()
                .forEach(header -> msg.append(header).append(": ").append(response.getHeader(header)).append("\n"));

        String contentType = response.getContentType();
        if (contentType == null) contentType = "";

        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0 && (contentType.startsWith("application/json") || contentType.startsWith("text/"))) {
            String body = new String(buf, StandardCharsets.UTF_8);
            msg.append("Body: ").append(body).append("\n");
        } else if (!contentType.isEmpty()) {
            msg.append("Non-text response body skipped: Content-Type=").append(contentType).append("\n");
        }

        System.out.println(msg);
    }
}
