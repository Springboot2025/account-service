package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String mobile;
    private String gender;
    private String address;
    private String accountType;

    // New fields
    private LocalDate dob;
    private boolean terms;
    private boolean newsletter;

    private Map<String, Object> addressDetails;
    private Map<String, Object> contactInformation;
    private Map<String, Object> emergencyContact;

    // Lawyer-only fields
    private String organization;
    private String experience;
    private String officeAddress;
    private String teamSize;
    private String languages;
}
