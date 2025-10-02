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
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .gender(account.getGender())
                .dateOfBirth(account.getDateOfBirth())
                .email(account.getEmail())
                .mobile(account.getMobile())
                .address(account.getAddress())
                .terms(account.isTerms())
                .newsletter(account.isNewsletter())
                .addressDetails(toMap(account.getAddressDetails()))
                .contactInformation(toMap(account.getContactInformation()))
                .emergencyContact(toMap(account.getEmergencyContact()))
                .newsletter(account.isNewsletter())
                .newsletter(account.isNewsletter())
                .build();
    }

    // --- Lawyer Mapping ---
    public static LawyerDto toLawyerDto(Account account) {
        if (account == null) return null;

        return LawyerDto.builder()
                .id(account.getId())
                .uuid(account.getUuid())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .gender(account.getGender())
                .dateOfBirth(account.getDateOfBirth())
                .email(account.getEmail())
                .mobile(account.getMobile())
                .address(account.getAddress())
                .terms(account.isTerms())
                .newsletter(account.isNewsletter())
                .organization(account.getOrganization())
                .experience(account.getExperience())
                .officeAddress(account.getOfficeAddress())
                .teamSize(account.getTeamSize())
                .languages(account.getLanguages())
                .build();
    }

    private static Map<String, Object> toMap(JsonNode node) {
        return node != null ? objectMapper.convertValue(node, new TypeReference<>() {}) : null;
    }

}
