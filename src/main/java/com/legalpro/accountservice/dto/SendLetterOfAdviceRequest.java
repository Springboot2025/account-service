package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendLetterOfAdviceRequest {
    private String recipientEmail;
    private String subject;
    private String message;
}
