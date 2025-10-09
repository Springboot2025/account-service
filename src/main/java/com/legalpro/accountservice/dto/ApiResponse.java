package com.legalpro.accountservice.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({ "code", "status", "message", "data" }) // enforce JSON field order
public class ApiResponse<T> {
    private int code;         // HTTP status code as number
    private String status;    // "success" / "error"
    private String message;   // description
    private T data;           // payload

    // ✅ Convenience method for quick success responses
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .status("success")
                .message("Success")
                .data(data)
                .build();
    }

    // ✅ Full success version with custom status + message
    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .status("success")
                .message(message)
                .data(data)
                .build();
    }

    // ✅ Error with payload
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .status("error")
                .message(message)
                .data(data)
                .build();
    }

    // ✅ Error without payload
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .status("error")
                .message(message)
                .data(null)
                .build();
    }
}
