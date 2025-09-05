package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.AccountDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final AccountRepository accountRepository;

    public List<AccountDto> getUsersByType(String userType) {
        String normalizedType = userType.trim().toLowerCase(Locale.ROOT);

        List<Account> accounts;
        switch (normalizedType) {
            case "client":
                accounts = accountRepository.findByRoleName("Client");
                break;
            case "lawyer":
                accounts = accountRepository.findByRoleName("Lawyer");
                break;
            default:
                throw new IllegalArgumentException("Invalid user type: " + userType);
        }

        return accounts.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .uuid(account.getUuid())   // ✅ UUID directly
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .gender(account.getGender())
                .mobile(account.getMobile())
                .address(account.getAddress())
                .build();
    }
}
