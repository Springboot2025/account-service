package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.*;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Company;
import com.legalpro.accountservice.entity.CompanyInvite;
import com.legalpro.accountservice.entity.Role;
import com.legalpro.accountservice.mapper.AccountMapper;
import com.legalpro.accountservice.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final CompanyInviteRepository companyInviteRepository;
    private final ClientAnswerRepository clientAnswerRepository;
    private final CourtSupportMaterialRepository courtSupportMaterialRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final QuoteRepository quoteRepository;

    private final LawyerRatingRepository lawyerRatingRepository;
    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com";

    public AccountService(AccountRepository accountRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          ObjectMapper objectMapper,
                          CompanyRepository companyRepository,
                          CompanyInviteRepository companyInviteRepository,
                          ClientAnswerRepository clientAnswerRepository,
                          CourtSupportMaterialRepository courtSupportMaterialRepository,
                          ClientDocumentRepository clientDocumentRepository,
                          QuoteRepository quoteRepository,
                          LawyerRatingRepository lawyerRatingRepository) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.companyRepository = companyRepository;
        this.companyInviteRepository = companyInviteRepository;
        this.clientAnswerRepository = clientAnswerRepository;
        this.courtSupportMaterialRepository = courtSupportMaterialRepository;
        this.clientDocumentRepository = clientDocumentRepository;
        this.quoteRepository = quoteRepository;
        this.lawyerRatingRepository = lawyerRatingRepository;
    }

    public Account register(RegisterRequest request) {

        // --- 🔹 0. If inviteToken exists → validate invite ---
        CompanyInvite invite = null;

        if (request.getInviteToken() != null && !request.getInviteToken().isBlank()) {

            invite = companyInviteRepository.findByToken(request.getInviteToken())
                    .orElseThrow(() -> new RuntimeException("Invalid invite token"));

            if (invite.isUsed()) {
                throw new RuntimeException("Invite link has already been used");
            }

            if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Invite link has expired");
            }

            // Email must match the invited email
            if (!invite.getEmail().equalsIgnoreCase(request.getEmail())) {
                throw new RuntimeException("Email does not match the invitation");
            }
        }

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
                .email(request.getEmail())
                .isVerified(false)
                .isActive(false)
                .verificationToken(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(Set.of(role));

        if (request.getPersonalDetails() != null) {
            accountBuilder.personalDetails(objectMapper.convertValue(request.getPersonalDetails(), JsonNode.class));
        }

        if (request.getContactInformation() != null) {
            accountBuilder.contactInformation(objectMapper.convertValue(request.getContactInformation(), JsonNode.class));
        }

        if (request.getAddressDetails() != null) {
            accountBuilder.addressDetails(objectMapper.convertValue(request.getAddressDetails(), JsonNode.class));
        }

        if (request.getPreferences() != null) {
            accountBuilder.preferences(objectMapper.convertValue(request.getPreferences(), JsonNode.class));
        }

        if (request.getEmergencyContact() != null) {
            accountBuilder.emergencyContact(objectMapper.convertValue(request.getEmergencyContact(), JsonNode.class));
        }

        // 4. Add lawyer-specific fields if accountType == Lawyer
        if ("Lawyer".equalsIgnoreCase(accountType)) {
            if (request.getProfessionalDetails() != null) {
                accountBuilder.professionalDetails(objectMapper.convertValue(request.getProfessionalDetails(), JsonNode.class));
            }

            if (request.getEducationQualification() != null) {
                accountBuilder.educationQualification(objectMapper.convertValue(request.getEducationQualification(), JsonNode.class));
            }

            if (request.getExperienceStaff() != null) {
                accountBuilder.experienceStaff(objectMapper.convertValue(request.getExperienceStaff(), JsonNode.class));
            }

            if (request.getAwardsAppreciations() != null) {
                accountBuilder.awardsAppreciations(objectMapper.convertValue(request.getAwardsAppreciations(), JsonNode.class));
            }
        }

        // --- 5. Handle company logic for lawyers ---
        if ("Lawyer".equalsIgnoreCase(accountType)) {

            if (invite != null) {
                // 🔹 Registration using invite
                accountBuilder
                        .isCompany(false)
                        .companyUuid(invite.getCompanyUuid());
            }
            else if (request.isCompany()) {
                if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
                    throw new RuntimeException("Company name is required for company lawyers");
                }

                // Create new company
                Company company = Company.builder()
                        .uuid(UUID.randomUUID())
                        .name(request.getCompanyName())
                        .description(request.getCompanyDescription())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                companyRepository.save(company);

                accountBuilder
                        .isCompany(true)
                        .uuid(UUID.randomUUID())
                        .companyUuid(company.getUuid());
            }
            else if (request.getCompanyUuid() != null) {
                // Manual company assignment
                accountBuilder
                        .isCompany(false)
                        .companyUuid(request.getCompanyUuid());
            }
            else {
                // Independent lawyer
                accountBuilder
                        .isCompany(false)
                        .companyUuid(null);
            }

        } else {
            // Non-lawyers
            accountBuilder
                    .isCompany(false)
                    .companyUuid(null);
        }

        Account account = accountBuilder.build();
        accountRepository.save(account);

        // --- 6. Mark invite as used ---
        if (invite != null) {
            invite.setUsed(true);
            invite.setUsedAt(LocalDateTime.now());
            companyInviteRepository.save(invite);
        }

        // 7. Send verification email
        String verificationUrl = "https://lawproject-nu.vercel.app/set-password?token=" + account.getVerificationToken();

        String bodyHtml =
                "<p>Hello " + account.getEmail() + ",</p>"
                        + "<p>Thanks for signing up to Boss Law Online Services.</p>"
                        + "<p>Click below to verify your email:</p>"
                        + "<a href=\"" + verificationUrl + "\">Verify your email</a>";

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

        // update email only if provided and different
        if (dto.getEmail() != null && !dto.getEmail().equals(account.getEmail())) {
            if (accountRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            account.setEmail(dto.getEmail());
        }

        if (dto.getPersonalDetails() != null) {
            JsonNode personalDetails = objectMapper.convertValue(dto.getPersonalDetails(), JsonNode.class);
            account.setPersonalDetails(personalDetails);
        }

        if (dto.getContactInformation() != null) {
            JsonNode contactInformation = objectMapper.convertValue(dto.getContactInformation(), JsonNode.class);
            account.setContactInformation(contactInformation);
        }

        if (dto.getAddressDetails() != null) {
            JsonNode addressDetails = objectMapper.convertValue(dto.getAddressDetails(), JsonNode.class);
            account.setAddressDetails(addressDetails);
        }

        if (dto.getPreferences() != null) {
            JsonNode preferences = objectMapper.convertValue(dto.getPreferences(), JsonNode.class);
            account.setPreferences(preferences);
        }

        if (dto.getEmergencyContact() != null) {
            JsonNode emergencyContact = objectMapper.convertValue(dto.getEmergencyContact(), JsonNode.class);
            account.setEmergencyContact(emergencyContact);
        }

        return accountRepository.save(account);
    }


    // --- Update Lawyer Account ---
    public Account updateAccount(UUID uuid, LawyerDto dto, UUID requesterUuid) {
        Account account = accountRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!uuid.equals(requesterUuid)) {
            throw new RuntimeException("You can only update your own profile");
        }

        // update email only if provided and different
        if (dto.getEmail() != null && !dto.getEmail().equals(account.getEmail())) {
            if (accountRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            account.setEmail(dto.getEmail());
        }

        if (dto.getPersonalDetails() != null) {
            JsonNode personalDetails = objectMapper.convertValue(dto.getPersonalDetails(), JsonNode.class);
            account.setPersonalDetails(personalDetails);
        }

        if (dto.getContactInformation() != null) {
            JsonNode contactInformation = objectMapper.convertValue(dto.getContactInformation(), JsonNode.class);
            account.setContactInformation(contactInformation);
        }

        if (dto.getAddressDetails() != null) {
            JsonNode addressDetails = objectMapper.convertValue(dto.getAddressDetails(), JsonNode.class);
            account.setAddressDetails(addressDetails);
        }

        if (dto.getPreferences() != null) {
            JsonNode preferences = objectMapper.convertValue(dto.getPreferences(), JsonNode.class);
            account.setPreferences(preferences);
        }

        if (dto.getProfessionalDetails() != null) {
            JsonNode professionalDetails = objectMapper.convertValue(dto.getProfessionalDetails(), JsonNode.class);
            account.setProfessionalDetails(professionalDetails);
        }

        if (dto.getEducationQualification() != null) {
            JsonNode educationQualification = objectMapper.convertValue(dto.getEducationQualification(), JsonNode.class);
            account.setEducationQualification(educationQualification);
        }

        if (dto.getExperienceStaff() != null) {
            JsonNode experienceStaff = objectMapper.convertValue(dto.getExperienceStaff(), JsonNode.class);
            account.setExperienceStaff(experienceStaff);
        }

        if (dto.getAwardsAppreciations() != null) {
            JsonNode awardsAppreciations = objectMapper.convertValue(dto.getAwardsAppreciations(), JsonNode.class);
            account.setAwardsAppreciations(awardsAppreciations);
        }

        if (dto.getConsultationRates() != null) {
            JsonNode consultationRates = objectMapper.convertValue(dto.getConsultationRates(), JsonNode.class);
            account.setConsultationRates(consultationRates);
        }

        if (dto.getLanguages() != null) {
            JsonNode languages = objectMapper.convertValue(dto.getLanguages(), JsonNode.class);
            account.setLanguages(languages);
        }

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

        String bodyHtml = "<p>Hello " + account.getEmail() + ",</p>"
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

    @Transactional(readOnly = true)
    public List<LawyerDto> getCompanyMembers(UUID companyUuid) {
        List<Account> members = accountRepository.findByCompanyUuid(companyUuid);

        return members.stream()
                .map(AccountMapper::toLawyerDto)
                .collect(Collectors.toList());
    }

    public ClientFullResponseDto getClientFullDetails(UUID clientUuid, UUID lawyerUuid) {

        // 1️⃣ Get profile (Account → ClientDto)
        Account account = accountRepository.findByUuid(clientUuid)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        ClientDto profileDto = AccountMapper.toClientDto(account);


        // 2️⃣ Get all question sets (Core, Offence, or any future type)
        List<ClientAnswerDto> questionDtos = clientAnswerRepository
                .findAllByClientUuidAndDeletedAtIsNull(clientUuid)   // ✅ correct method
                .stream()
                .map(answer -> ClientAnswerDto.builder()
                        .id(answer.getId())
                        .clientUuid(answer.getClientUuid())
                        .questionType(answer.getQuestionType())
                        .answers(answer.getAnswers())
                        .createdAt(answer.getCreatedAt())
                        .updatedAt(answer.getUpdatedAt())
                        .build()
                )
                .toList();

        // 3️⃣ Get court support materials
        List<CourtSupportMaterialDto> courtMaterialDtos = courtSupportMaterialRepository
                .findByClientUuidAndDeletedAtIsNull(clientUuid)
                .stream()
                .map(item -> CourtSupportMaterialDto.builder()
                        .id(item.getId())
                        .clientUuid(item.getClientUuid())
                        .fileName(item.getFileName())
                        .fileType(item.getFileType())
                        .fileUrl(item.getFileUrl())
                        .description(item.getDescription())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build()
                )
                .toList();


        // 4️⃣ Get uploaded documents
        List<ClientDocumentDto> documentDtos = clientDocumentRepository
                .findByClientUuidAndDeletedAtIsNull(clientUuid)
                .stream()
                .map(doc -> ClientDocumentDto.builder()
                        .id(doc.getId())
                        .clientUuid(doc.getClientUuid())
                        .lawyerUuid(doc.getLawyerUuid())
                        .fileName(doc.getFileName())
                        .fileType(doc.getFileType())
                        .fileUrl(convertGcsUrl(doc.getFileUrl()))
                        .documentType(doc.getDocumentType())
                        .createdAt(doc.getCreatedAt())
                        .updatedAt(doc.getUpdatedAt())
                        .build()
                )
                .toList();

        // Get quotes
        List<QuoteDto> quoteDtos = quoteRepository
                .findByClientUuidAndLawyerUuid(clientUuid, lawyerUuid)
                .stream()
                .map(quote -> QuoteDto.builder()
                        .id(quote.getId())
                        .uuid(quote.getUuid())
                        .clientUuid(quote.getClientUuid())
                        .lawyerUuid(quote.getLawyerUuid())
                        .title(quote.getTitle())
                        .description(quote.getDescription())
                        .expectedAmount(quote.getExpectedAmount())
                        .quotedAmount(quote.getQuotedAmount())
                        .currency(quote.getCurrency())
                        .offenceList(quote.getOffenceList())
                        .status(quote.getStatus())
                        .createdAt(quote.getCreatedAt())
                        .updatedAt(quote.getUpdatedAt())
                        .build()
                )
                .toList();

        // 5️⃣ Stitch everything into final response DTO
        return ClientFullResponseDto.builder()
                .profileData(profileDto)
                .questions(questionDtos)
                .courtSupportingMaterial(courtMaterialDtos)
                .documents(documentDtos)
                .quotes(quoteDtos)
                .build();
    }

    private String convertGcsUrl(String fileUrl) {
        if (fileUrl != null && fileUrl.startsWith("gs://")) {
            return GCS_PUBLIC_BASE + "/" + fileUrl.substring("gs://".length());
        }
        return fileUrl;
    }

    @Transactional(readOnly = true)
    public PublicLawyerProfileDto getPublicLawyerProfile(UUID lawyerUuid) {

        // 1️⃣ Fetch account
        Account account = accountRepository.findByUuid(lawyerUuid)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        // 2️⃣ Ensure this account is a LAWYER
        boolean isLawyer = account.getRoles()
                .stream()
                .anyMatch(role -> "Lawyer".equalsIgnoreCase(role.getName()));

        if (!isLawyer) {
            throw new RuntimeException("Account is not a lawyer");
        }

        // 3️⃣ Rating summary
        BigDecimal averageRating =
                lawyerRatingRepository.findAverageRatingByLawyerUuid(lawyerUuid);

        if (averageRating == null) {
            averageRating = BigDecimal.ZERO;
        }

        int reviewCount = lawyerRatingRepository
                .findAllByLawyerUuid(lawyerUuid)
                .stream()
                .filter(r -> r.getDeletedAt() == null)
                .toList()
                .size();

        // 4️⃣ Build public DTO
        return PublicLawyerProfileDto.from(
                account,
                averageRating,
                reviewCount
        );
    }

}
