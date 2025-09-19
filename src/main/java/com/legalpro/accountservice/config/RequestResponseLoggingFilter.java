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
        // Wrap request/response to allow multiple reads
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            logRequest(wrappedRequest);
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse(); // important!
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        StringBuilder msg = new StringBuilder();

        msg.append("\n--- Incoming Request ---\n");
        msg.append(request.getMethod()).append(" ").append(request.getRequestURI()).append("\n");

        Collections.list(request.getHeaderNames()).forEach(header ->
                msg.append(header).append(": ").append(request.getHeader(header)).append("\n")
        );

        if (!request.getContentType().contains("multipart/form-data")) {
            String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (!body.isBlank()) {
                msg.append("Body: ").append(body).append("\n");
            }
        } else {
            msg.append("Multipart request: skipping raw body logging\n");
        }

        System.out.println(msg);
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        StringBuilder msg = new StringBuilder();

        msg.append("\n--- Outgoing Response ---\n");
        msg.append("Status: ").append(response.getStatus()).append("\n");

        response.getHeaderNames().forEach(header ->
                msg.append(header).append(": ").append(response.getHeader(header)).append("\n")
        );

        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        if (!body.isBlank()) {
            msg.append("Body: ").append(body).append("\n");
        }

        System.out.println(msg);
    }
}
