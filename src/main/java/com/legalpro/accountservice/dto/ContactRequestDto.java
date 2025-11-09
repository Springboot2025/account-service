package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContactRequestDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String firmName;
    private String message;
    private boolean subscribeNewsletter;
}
