package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.AccountDto;
import com.legalpro.accountservice.dto.RegisterRequest;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Role;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Account register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        String accountType = request.getAccountType() != null ? request.getAccountType() : "Client";
        Role role = roleRepository.findByName(accountType)
                .orElseThrow(() -> new RuntimeException("Role not found: " + accountType));

        Account account = Account.builder()
                .uuid(UUID.randomUUID())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .mobile(request.getMobile())
                .gender(request.getGender())
                .address(request.getAddress())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(Set.of(role))
                .build();

        return accountRepository.save(account);
    }

    // --- NEW METHODS FOR CLIENT PATCH ---
    public Optional<Account> findByUuid(UUID uuid) {
        return accountRepository.findByUuid(uuid);
    }

    public Account save(Account account) {
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    // Inside AccountService.java
    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }

    public Account updateAccount(UUID uuid, AccountDto dto, UUID requesterUuid) {
        // Ownership check
        if (!uuid.equals(requesterUuid)) {
            throw new RuntimeException("You can only update your own profile");
        }

        Account account = findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (dto.getEmail() != null && !dto.getEmail().equals(account.getEmail())) {
            if (existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Email is already in use");
            }
            account.setEmail(dto.getEmail());
        }

        if (dto.getMobile() != null) account.setMobile(dto.getMobile());
        if (dto.getAddress() != null) account.setAddress(dto.getAddress());

        return save(account);
    }

    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

}
