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

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .status("success")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .status("error")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .status("error")
                .message(message)
                .data(null)
                .build();
    }
}
