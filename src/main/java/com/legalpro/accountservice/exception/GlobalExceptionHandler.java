package com.legalpro.accountservice.exception;

import com.legalpro.accountservice.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_SERVER_ERROR_MESSAGE =
            "Something went wrong on our end. Please try again, or contact support if this continues.";

    private static final Pattern CONSTRAINT_PATTERN = Pattern.compile("constraint\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    // Handle @Valid validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        ApiResponse<Map<String, String>> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle invalid parameter types (e.g., bad UUID format)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String message = String.format("Invalid value '%s' for parameter '%s'", invalidValue, paramName);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    // Handle Spring Security: unauthorized (JWT invalid, expired, etc.)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<String>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: " + ex.getMessage()));
    }

    // Handle Spring Security: forbidden (no access rights)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Forbidden: " + ex.getMessage()));
    }

    // Handle runtime (business) exceptions. IllegalArgumentException/
    // IllegalStateException are the ones our own code throws deliberately as
    // user-facing validation messages (e.g. "Unsupported file type") -- safe
    // to return as-is. Anything else is unexpected: the real detail (which
    // can include SQL fragments, internal class names, file paths) goes to
    // the server log only, never to the client.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
        }
        log.error("Unhandled RuntimeException", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), GENERIC_SERVER_ERROR_MESSAGE));
    }

    // Generic fallback for any other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {
        log.error("Unhandled Exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), GENERIC_SERVER_ERROR_MESSAGE));
    }

    // Handle endpoint not found (404)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(NoHandlerFoundException ex) {
        ApiResponse<String> response = ApiResponse.error(
                HttpStatus.NOT_FOUND.value(),
                "Endpoint not found"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Handle DB constraint errors (duplicate entries, etc.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootCause = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        String constraintName = extractConstraintName(rootCause);
        String message;

        // Known constraints get a specific, safe, user-facing message. Any
        // other constraint name (or none at all) falls back to a generic
        // message -- the raw constraint/rootCause text can reveal table and
        // column names and isn't meant for API clients. Full detail is
        // still logged server-side below either way.
        if (constraintName != null) {
            switch (constraintName) {
                case "uq_client_file":
                    message = "Duplicate entry: This client already has a file with the same name.";
                    break;
                case "uq_accounts_email":
                    message = "Duplicate entry: An account with this email already exists.";
                    break;
                case "accounts_pkey":
                    message = "Duplicate entry: Account with this ID already exists.";
                    break;
                case "client_answers_unique":
                    message = "Duplicate entry: An answer already exists for this client and question type.";
                    break;
                default:
                    message = "This request conflicts with existing data.";
            }
        } else {
            message = "This request conflicts with existing data.";
        }

        log.warn("DataIntegrityViolationException: constraint={}, rootCause={}", constraintName, rootCause);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT.value(), message));
    }

    private String extractConstraintName(String rootCause) {
        if (rootCause == null) return null;
        Matcher matcher = CONSTRAINT_PATTERN.matcher(rootCause);
        return matcher.find() ? matcher.group(1) : null;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatusException(
            ResponseStatusException ex) {

        return ResponseEntity.status(ex.getStatusCode())
                .body(
                        ApiResponse.error(
                                ex.getStatusCode().value(),
                                ex.getReason()
                        )
                );
    }
}
