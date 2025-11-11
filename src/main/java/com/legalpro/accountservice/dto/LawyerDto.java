package com.legalpro.accountservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawyerDto {
    private Long id;
    private UUID uuid;

    private String email;
    private Map<String, Object> personalDetails;
    private Map<String, Object> contactInformation;
    private Map<String, Object> addressDetails;
    private Map<String, Object> preferences;
    private Map<String, Object> professionalDetails;

    private List<Map<String, Object>> educationQualification;
    private List<Map<String, Object>> experienceStaff;
    private List<Map<String, Object>> awardsAppreciations;

    private boolean isCompany;
    private UUID companyUuid;
    private BigDecimal averageRating;
}
