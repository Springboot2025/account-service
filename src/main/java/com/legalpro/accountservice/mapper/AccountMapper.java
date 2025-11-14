package com.legalpro.accountservice.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.ClientDto;
import com.legalpro.accountservice.dto.LawyerDto;
import com.legalpro.accountservice.entity.Account;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class AccountMapper {
    private static ObjectMapper objectMapper = new ObjectMapper();
    // --- Client Mapping ---
    public static ClientDto toClientDto(Account account) {
        if (account == null) return null;

        return ClientDto.builder()
                .id(account.getId())
                .uuid(account.getUuid())
                .email(account.getEmail())
                .personalDetails(toMap(account.getPersonalDetails()))
                .contactInformation(toMap(account.getContactInformation()))
                .addressDetails(toMap(account.getAddressDetails()))
                .preferences(toMap(account.getPreferences()))
                .emergencyContact(toMap(account.getEmergencyContact()))
                .profilePictureUrl(account.getProfilePictureUrl())
                .build();
    }

    // --- Lawyer Mapping ---
    public static LawyerDto toLawyerDto(Account account) {
        if (account == null) return null;

        return LawyerDto.builder()
                .id(account.getId())
                .uuid(account.getUuid())
                .email(account.getEmail())
                .personalDetails(toMap(account.getPersonalDetails()))
                .contactInformation(toMap(account.getContactInformation()))
                .addressDetails(toMap(account.getAddressDetails()))
                .preferences(toMap(account.getPreferences()))
                .professionalDetails(toMap(account.getProfessionalDetails()))
                .educationQualification(toList(account.getEducationQualification()))
                .experienceStaff(toList(account.getExperienceStaff()))
                .awardsAppreciations(toList(account.getAwardsAppreciations()))
                .profilePictureUrl(account.getProfilePictureUrl())
                .isCompany(account.isCompany())
                .companyUuid(account.getCompanyUuid())
                .build();
    }

    private static Map<String, Object> toMap(JsonNode node) {
        return node != null ? objectMapper.convertValue(node, new TypeReference<>() {}) : null;
    }

    private static java.util.List<java.util.Map<String, Object>> toList(JsonNode node) {
        if (node == null) return null;

        // If stored JSON is already an array → convert directly
        if (node.isArray()) {
            return objectMapper.convertValue(
                    node,
                    new TypeReference<java.util.List<java.util.Map<String, Object>>>() {}
            );
        }

        // If stored JSON is a single object → wrap it in a list
        Map<String, Object> single = objectMapper.convertValue(
                node,
                new TypeReference<java.util.Map<String, Object>>() {}
        );
        return java.util.List.of(single);
    }


}
