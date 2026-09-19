package com.legalpro.accountservice.dto;

import lombok.Data;

@Data
public class LetterOfAdviceChangeRequestCreateRequest {
    private String category;
    private String message;
}
