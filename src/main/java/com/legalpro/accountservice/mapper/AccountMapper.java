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
                .educationQualification(toMap(account.getEducationQualification()))
                .experienceStaff(toMap(account.getExperienceStaff()))
                .awardsAppreciations(toMap(account.getAwardsAppreciations()))
                .build();
    }

    private static Map<String, Object> toMap(JsonNode node) {
        return node != null ? objectMapper.convertValue(node, new TypeReference<>() {}) : null;
    }

}
