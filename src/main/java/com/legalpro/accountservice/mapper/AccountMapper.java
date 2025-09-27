package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.ClientDto;
import com.legalpro.accountservice.dto.LawyerDto;
import com.legalpro.accountservice.entity.Account;

public class AccountMapper {

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
}
