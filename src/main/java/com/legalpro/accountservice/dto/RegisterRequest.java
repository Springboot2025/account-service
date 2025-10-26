package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String email;
    private String accountType;

    private Map<String, Object> personalDetails;
    private Map<String, Object> contactInformation;
    private Map<String, Object> addressDetails;
    private Map<String, Object> preferences;
    private Map<String, Object> emergencyContact;
    private Map<String, Object> professionalDetails;
    private Map<String, Object> educationQualification;
    private Map<String, Object> experienceStaff;
    private Map<String, Object> awardsAppreciations;
    private boolean isCompany;
    private UUID companyUuid;
}
