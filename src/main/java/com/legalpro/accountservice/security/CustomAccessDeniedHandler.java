package com.legalpro.accountservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");

        ApiResponse<String> apiResponse = ApiResponse.error(
                HttpStatus.FORBIDDEN.value(),
                accessDeniedException.getMessage()
        );

        response.getWriter().write(
                objectMapper.writeValueAsString(apiResponse)
        );
    }
}
