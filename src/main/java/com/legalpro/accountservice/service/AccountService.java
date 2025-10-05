package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.AccountDto;
import com.legalpro.accountservice.dto.ClientDto;
import com.legalpro.accountservice.dto.LawyerDto;
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
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public AccountService(AccountRepository accountRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    public Account register(RegisterRequest request) {
        // 1. Validate if email already exists
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // 2. Resolve role (default = Client)
        String accountType = request.getAccountType() != null ? request.getAccountType() : "Client";
        Role role = roleRepository.findByName(accountType)
                .orElseThrow(() -> new RuntimeException("Role not found: " + accountType));

        // 3. Build Account entity with common fields
        Account.AccountBuilder accountBuilder = Account.builder()
                .uuid(UUID.randomUUID())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .gender(request.getGender())
                .address(request.getAddress())
                .dateOfBirth(request.getDob())
                .terms(request.isTerms())
                .newsletter(request.isNewsletter())
                .isVerified(false)
                .isActive(false)
                .verificationToken(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(Set.of(role));

        if (request.getAddressDetails() != null) {
            accountBuilder.addressDetails(objectMapper.convertValue(request.getAddressDetails(), JsonNode.class));
        }
        if (request.getContactInformation() != null) {
            accountBuilder.contactInformation(objectMapper.convertValue(request.getContactInformation(), JsonNode.class));
        }
        if (request.getEmergencyContact() != null) {
            accountBuilder.emergencyContact(objectMapper.convertValue(request.getEmergencyContact(), JsonNode.class));
        }

        // 4. Add lawyer-specific fields if accountType == Lawyer
        if ("Lawyer".equalsIgnoreCase(accountType)) {
            accountBuilder
                    .organization(request.getOrganization())
                    .experience(request.getExperience())
                    .officeAddress(request.getOfficeAddress())
                    .teamSize(request.getTeamSize())
                    .languages(request.getLanguages());
        }

        Account account = accountBuilder.build();
        accountRepository.save(account);

        // 5. Send verification email
        String verificationUrl = "https://lawproject-nu.vercel.app/set-password?token="
                + account.getVerificationToken();

        String bodyHtml = "<p>Hello " + account.getFirstName() + ",</p>"
                + "<p>Thanks for signing up to Boss Law Online Services. "
                + "Click the link below to verify your email address.</p>"
                + "<a href=\"" + verificationUrl + "\">Click here to verify your email address.</a>";

        emailService.sendEmail(account.getEmail(), "Boss Law Verification", bodyHtml);

        return account;
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

    public Account updateAccount(UUID uuid, ClientDto dto, UUID requesterUuid) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!uuid.equals(requesterUuid)) {
            throw new RuntimeException("You can only update your own profile");
        }

        if (dto.getFirstName() != null) account.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) account.setLastName(dto.getLastName());
        if (dto.getGender() != null) account.setGender(dto.getGender());
        if (dto.getDateOfBirth() != null) account.setDateOfBirth(dto.getDateOfBirth());

        // update email only if provided and different
        if (dto.getEmail() != null && !dto.getEmail().equals(account.getEmail())) {
            if (accountRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            account.setEmail(dto.getEmail());
        }

        if (dto.getMobile() != null) account.setMobile(dto.getMobile());
        if (dto.getAddress() != null) account.setAddress(dto.getAddress());

        if (dto.getAddressDetails() != null) {
            JsonNode addressDetails = objectMapper.convertValue(dto.getAddressDetails(), JsonNode.class);
            account.setAddressDetails(addressDetails);
        }
        if (dto.getContactInformation() != null) {
            JsonNode contactInformation = objectMapper.convertValue(dto.getContactInformation(), JsonNode.class);
            account.setContactInformation(contactInformation);
        }
        if (dto.getEmergencyContact() != null) {
            JsonNode emergencyContact = objectMapper.convertValue(dto.getEmergencyContact(), JsonNode.class);
            account.setEmergencyContact(emergencyContact);
        }

        // booleans: only update if explicitly set in request
        account.setTerms(dto.isTerms());
        account.setNewsletter(dto.isNewsletter());

        return accountRepository.save(account);
    }


    // --- Update Lawyer Account ---
    public Account updateAccount(UUID uuid, LawyerDto dto, UUID requesterUuid) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!uuid.equals(requesterUuid)) {
            throw new RuntimeException("You can only update your own profile");
        }

        if (dto.getFirstName() != null) account.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) account.setLastName(dto.getLastName());
        if (dto.getGender() != null) account.setGender(dto.getGender());
        if (dto.getDateOfBirth() != null) account.setDateOfBirth(dto.getDateOfBirth());

        // update email only if provided and different
        if (dto.getEmail() != null && !dto.getEmail().equals(account.getEmail())) {
            if (accountRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            account.setEmail(dto.getEmail());
        }

        if (dto.getMobile() != null) account.setMobile(dto.getMobile());
        if (dto.getAddress() != null) account.setAddress(dto.getAddress());
        if (dto.getOrganization() != null) account.setOrganization(dto.getOrganization());
        if (dto.getExperience() != null) account.setExperience(dto.getExperience());
        if (dto.getOfficeAddress() != null) account.setOfficeAddress(dto.getOfficeAddress());
        if (dto.getTeamSize() != null) account.setTeamSize(dto.getTeamSize());
        if (dto.getLanguages() != null) account.setLanguages(dto.getLanguages());

        // booleans — update directly (defaults should be handled at entity level)
        account.setTerms(dto.isTerms());
        account.setNewsletter(dto.isNewsletter());

        return accountRepository.save(account);
    }


    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    public Optional<Account> verifyAccount(UUID token) {
        Optional<Account> accountOpt = accountRepository.findByVerificationToken(token);
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }

        Account account = accountOpt.get();
        account.setVerified(true);
        account.setActive(true);
        accountRepository.save(account);

        return Optional.of(account);
    }


    public Optional<Account> findByVerificationToken(UUID token) {
        return accountRepository.findByVerificationToken(token);
    }

    public void setPassword(UUID uuid, String rawPassword) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Invalid account UUID"));

        if (!account.isVerified()) {
            throw new RuntimeException("Account is not verified yet");
        }

        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setUpdatedAt(LocalDateTime.now());
        account.setVerificationToken(null);
        accountRepository.save(account);
    }

    public void sendForgotPasswordEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        UUID token = UUID.randomUUID();
        account.setForgotPasswordToken(token);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        String resetUrl = "https://lawproject-nu.vercel.app/reset-password?token=" + token;

        String bodyHtml = "<p>Hello " + account.getFirstName() + ",</p>"
                + "<p>You requested to reset your password. Click below:</p>"
                + "<a href=\"" + resetUrl + "\">Reset Password</a>";

        emailService.sendEmail(account.getEmail(), "Password Reset Request", bodyHtml);
    }

    public void resetPassword(UUID token, String newPassword) {
        Account account = accountRepository.findByForgotPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setForgotPasswordToken(null);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

}
