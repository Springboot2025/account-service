package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.*;
import com.legalpro.accountservice.dto.admin.*;
import com.legalpro.accountservice.dto.ClientDto;
import com.legalpro.accountservice.dto.admin.AdminClientDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Company;
import com.legalpro.accountservice.entity.CompanyInvite;
import com.legalpro.accountservice.entity.Role;
import com.legalpro.accountservice.enums.AccountStatus;
import com.legalpro.accountservice.mapper.AccountMapper;
import com.legalpro.accountservice.repository.*;
import com.legalpro.accountservice.security.CustomUserDetails;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.specification.CaseSpecification;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final LegalCaseRepository legalCaseRepository;
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
                          LawyerRatingRepository lawyerRatingRepository,
                          LegalCaseRepository legalCaseRepository) {
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
        this.legalCaseRepository = legalCaseRepository;
    }

    public Account register(RegisterRequest request) throws IOException {

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

        /*String bodyHtml =
                "<p>Hello " + account.getEmail() + ",</p>"
                        + "<p>Thanks for signing up to Boss Law Online Services.</p>"
                        + "<p>Click below to verify your email:</p>"
                        + "<a href=\"" + verificationUrl + "\">Verify your email</a>";*/

        ClassPathResource resource = new ClassPathResource("templates/register.html");
        String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String fullName = extractFullName(account);

        String bodyHtml = template
                .replace("${userEmail}", account.getEmail())
                .replace("${verificationLink}", verificationUrl)
                .replace("${userName}", fullName);

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

        if (dto.getLanguages() != null) {
            JsonNode languages = objectMapper.convertValue(dto.getLanguages(), JsonNode.class);
            account.setLanguages(languages);
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

    public void sendForgotPasswordEmail(String email) throws IOException{
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        UUID token = UUID.randomUUID();
        account.setForgotPasswordToken(token);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        String resetUrl = "https://lawproject-nu.vercel.app/reset-password?token=" + token;

        /*String bodyHtml = "<p>Hello " + account.getEmail() + ",</p>"
                + "<p>You requested to reset your password. Click below:</p>"
                + "<a href=\"" + resetUrl + "\">Reset Password</a>";*/

        ClassPathResource resource = new ClassPathResource("templates/reset-password.html");
        String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String fullName = extractFullName(account);

        String bodyHtml = template
                .replace("${userEmail}", account.getEmail())
                .replace("${resetUrl}", resetUrl)
                .replace("${userName}", fullName);

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
                        .fileUrl(convertGcsUrl(item.getFileUrl()))
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

    public ClientFullResponseDto getClientFullDetails(UUID clientUuid) {

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
                        .fileUrl(convertGcsUrl(item.getFileUrl()))
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
                .findByClientUuid(clientUuid)
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

    private String extractFullName(Account account) {
        if (account == null || account.getPersonalDetails() == null) return "";

        JsonNode pd = account.getPersonalDetails();
        String first = pd.hasNonNull("firstName") ? pd.get("firstName").asText() : "";
        String last  = pd.hasNonNull("lastName")  ? pd.get("lastName").asText()  : "";

        return (first + " " + last).trim();
    }

    public InvitationSummaryDto getInvitationSummary(CustomUserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();

        Account account = accountRepository.findByUuid(userDetails.getUuid())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID companyUuid = account.isCompany()
                ? account.getCompanyUuid()
                : account.getUuid();

        long totalSent = companyInviteRepository.countByCompanyUuid(companyUuid);

        long accepted = companyInviteRepository.countByCompanyUuidAndUsedTrue(companyUuid);

        long pending = companyInviteRepository
                .countByCompanyUuidAndUsedFalseAndExpiresAtAfter(companyUuid, now);

        long expired = companyInviteRepository
                .countByCompanyUuidAndUsedFalseAndExpiresAtBefore(companyUuid, now);

        long declined = 0;

        return InvitationSummaryDto.builder()
                .totalSent(totalSent)
                .pending(pending)
                .accepted(accepted)
                .declined(declined)
                .expired(expired)
                .build();
    }

    public InvitationListResponse getInvitations(
            String status,
            int page,
            int size,
            CustomUserDetails userDetails
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Account account = accountRepository.findByUuid(userDetails.getUuid())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID companyUuid = account.isCompany()
                ? account.getCompanyUuid()
                : account.getUuid();

        LocalDateTime now = LocalDateTime.now();

        Specification<CompanyInvite> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // company filter
            predicates.add(cb.equal(root.get("companyUuid"), companyUuid));

            // status filter
            switch (status.toUpperCase()) {

                case "PENDING":
                    predicates.add(cb.isFalse(root.get("used")));
                    predicates.add(cb.greaterThan(root.get("expiresAt"), now));
                    break;

                case "ACCEPTED":
                    predicates.add(cb.isTrue(root.get("used")));
                    break;

                case "EXPIRED":
                    predicates.add(cb.isFalse(root.get("used")));
                    predicates.add(cb.lessThan(root.get("expiresAt"), now));
                    break;

                case "DECLINED":
                    // placeholder → return empty
                    predicates.add(cb.disjunction());
                    break;

                default:
                    // ALL → no extra filter
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CompanyInvite> invitePage = companyInviteRepository.findAll(spec, pageable);

        List<InvitationDto> content = invitePage.getContent()
                .stream()
                .map(invite -> mapToInvitationDto(invite, now))
                .toList();

        return InvitationListResponse.builder()
                .content(content)
                .page(invitePage.getNumber())
                .size(invitePage.getSize())
                .totalElements(invitePage.getTotalElements())
                .totalPages(invitePage.getTotalPages())
                .build();
    }

    private InvitationDto mapToInvitationDto(CompanyInvite invite, LocalDateTime now) {

        String status;

        if (invite.isUsed()) {
            status = "ACCEPTED";
        } else if (invite.getExpiresAt().isBefore(now)) {
            status = "EXPIRED";
        } else {
            status = "PENDING";
        }

        return InvitationDto.builder()
                .uuid(invite.getUuid())
                .email(invite.getEmail())
                .status(status)
                .sentAt(invite.getCreatedAt())
                .expiresAt(invite.getExpiresAt())
                .usedAt(invite.getUsedAt())
                .role("Associate") // placeholder
                .specialization("N/A") // placeholder
                .build();
    }

    public void cancelInvitation(UUID inviteUuid, CustomUserDetails userDetails) {

        Account account = accountRepository.findByUuid(userDetails.getUuid())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID companyUuid = account.isCompany()
                ? account.getCompanyUuid()
                : account.getUuid();

        CompanyInvite invite = companyInviteRepository.findByUuid(inviteUuid)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invite.getCompanyUuid().equals(companyUuid)) {
            throw new RuntimeException("Not allowed");
        }

        if (invite.isUsed()) {
            throw new RuntimeException("Cannot cancel accepted invitation");
        }

        companyInviteRepository.delete(invite);
    }

    public FirmDashboardSummaryDto getFirmSummary(CustomUserDetails userDetails) {
        Account account = accountRepository.findByUuid(userDetails.getUuid())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID companyUuid = account.isCompany()
                ? account.getCompanyUuid()
                : account.getUuid();

        long activeLawyers = accountRepository.count(
                (root, query, cb) -> {
                    query.distinct(true);
                    return cb.and(
                            cb.equal(root.join("roles").get("name"), "Lawyer"),
                            cb.isFalse(root.get("isCompany")),
                            cb.equal(root.get("companyUuid"), companyUuid),
                            cb.equal(root.get("accountStatus"), AccountStatus.ACTIVE)
                    );
                }
        );

        long totalCases = legalCaseRepository.countCasesByCompanyUuid(companyUuid);
        long pendingInvites = companyInviteRepository.countByCompanyUuidAndUsedFalse(companyUuid);

        return FirmDashboardSummaryDto.builder()
                .activeLawyers(activeLawyers)
                .totalCases(totalCases)
                .pendingInvites(pendingInvites)
                .performance(0.0)
                .build();
    }

    public AdminUserListResponse getFormUsers(
            String search,
            String status,
            String location,
            String sort,
            int page,
            int size,
            CustomUserDetails userDetails
    ) {
        Sort sortOrder;
        String type = "LAWYER";

        Account account = accountRepository.findByUuid(userDetails.getUuid())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID companyUuid = account.isCompany()
                ? account.getCompanyUuid()
                : account.getUuid();

        switch (sort.toUpperCase()) {
            case "OLDEST":
                sortOrder = Sort.by("createdAt").ascending();
                break;

            case "NAME":
                sortOrder = Sort.by("email").ascending();
                break;

            case "MOST_ACTIVE":
                sortOrder = Sort.by("createdAt").descending(); // placeholder
                break;

            default:
                sortOrder = Sort.by("createdAt").descending(); // NEWEST
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<Account> accounts;

        switch (type.toUpperCase()) {
            case "LAWYER":
                accounts = accountRepository.findAll(
                        (root, query, cb) -> {
                            List<Predicate> predicates = new ArrayList<>();
                            predicates.add(cb.equal(root.join("roles").get("name"), "Lawyer"));
                            predicates.add(cb.isFalse(root.get("isCompany")));
                            predicates.add(buildSearchPredicate(root, cb, search));

                            if (companyUuid != null) {
                                predicates.add(cb.equal(root.get("companyUuid"), companyUuid));
                            }

                            return cb.and(predicates.toArray(new Predicate[0]));
                        },
                        pageable
                );
                break;
            default:
                accounts = accountRepository.findAll(
                        (root, query, cb) -> {

                            if (search == null || search.isBlank()) {
                                return cb.conjunction();
                            }

                            String like = "%" + search.toLowerCase() + "%";

                            return cb.or(
                                    cb.like(cb.lower(root.get("email")), like),
                                    cb.like(
                                            cb.lower(cb.function(
                                                    "jsonb_extract_path_text",
                                                    String.class,
                                                    root.get("personalDetails"),
                                                    cb.literal("firstName")
                                            )),
                                            like
                                    ),
                                    cb.like(
                                            cb.lower(cb.function(
                                                    "jsonb_extract_path_text",
                                                    String.class,
                                                    root.get("personalDetails"),
                                                    cb.literal("lastName")
                                            )),
                                            like
                                    )
                            );
                        },
                        pageable
                );
        }

        List<AdminUserDto> users = accounts.getContent()
                .stream()
                .map(this::mapToAdminUserDto)
                .toList();

        return AdminUserListResponse.builder()
                .users(users)
                .page(accounts.getNumber())
                .size(accounts.getSize())
                .totalElements(accounts.getTotalElements())
                .totalPages(accounts.getTotalPages())
                .build();
    }

    private AdminUserDto mapToAdminUserDto(Account account) {

        String name = "";

        if (account.getPersonalDetails() != null) {
            String firstName = account.getPersonalDetails().path("firstName").asText("");
            String lastName = account.getPersonalDetails().path("lastName").asText("");
            name = (firstName + " " + lastName).trim();
        }

        String role = account.getRoles()
                .stream()
                .findFirst()
                .map(r -> r.getName())
                .orElse("");

        if (Boolean.TRUE.equals(account.isCompany())) {
            role = "Firm";
        }

        String profilePictureUrl = convertGcsUrl(account.getProfilePictureUrl());

        String location = "";

        if (account.getAddressDetails() != null) {

            String city =
                    account.getAddressDetails()
                            .path("city_suburb")
                            .asText("");

            String state =
                    account.getAddressDetails()
                            .path("state_province")
                            .asText("");

            location = (city + ", " + state).trim();
        }

        String status = account.isActive() ? "Active" : "Inactive";
        AccountStatus accountStatus = account.getAccountStatus();

        String specialization = "";

        if (account.getProfessionalDetails() != null) {
            specialization = account.getProfessionalDetails()
                    .path("practiceArea")
                    .asText("");
        }

        LocalDateTime joinedAt = account.getCreatedAt();

        int lawyerCount = 0;

        if (Boolean.TRUE.equals(account.isCompany())) {
            lawyerCount = accountRepository.countByCompanyUuid(account.getUuid());

            if (lawyerCount == 0) {
                lawyerCount = 1;
            }
        }

        double rating = 0;

        boolean isLawyer =
                account.getRoles()
                        .stream()
                        .anyMatch(r -> r.getName().equalsIgnoreCase("Lawyer"));

        if (isLawyer && !Boolean.TRUE.equals(account.isCompany())) {

            BigDecimal avgRating =
                    lawyerRatingRepository.findAverageRatingByLawyerUuid(account.getUuid());

            rating = avgRating != null ? avgRating.doubleValue() : 0;
        }

        int cases = 0;

        boolean isClient =
                account.getRoles()
                        .stream()
                        .anyMatch(r -> r.getName().equalsIgnoreCase("Client"));

        if (isClient) {
            cases = (int) legalCaseRepository.countByClientUuid(account.getUuid());
        }

        if (isLawyer && !Boolean.TRUE.equals(account.isCompany())) {
            cases = (int) legalCaseRepository.countByLawyerUuid(account.getUuid());
        }

        if (account.isCompany()) {
            cases = (int) legalCaseRepository.countCompanyCases(account.getUuid());
        }

        return AdminUserDto.builder()
                .uuid(account.getUuid())
                .name(name)
                .email(account.getEmail())
                .role(role)
                .location(location)
                .status(status)
                .accountStatus(accountStatus)
                .cases(cases)
                .rating(rating)
                .spent(0)
                .earned(0)
                .specialization(specialization)
                .joinedAt(joinedAt)
                .lawyerCount(lawyerCount)
                .profilePictureUrl(profilePictureUrl)
                .build();
    }

    private Predicate buildSearchPredicate(Root<Account> root, CriteriaBuilder cb, String search) {

        if (search == null || search.isBlank()) {
            return cb.conjunction();
        }

        String like = "%" + search.toLowerCase() + "%";

        return cb.or(
                cb.like(cb.lower(root.get("email")), like),
                cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                root.get("personalDetails"),
                                cb.literal("firstName")
                        )),
                        like
                ),
                cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                root.get("personalDetails"),
                                cb.literal("lastName")
                        )),
                        like
                )
        );
    }

    public AdminCaseListResponse getFirmCases(
            String search,
            String status,
            String type,
            String sort,
            int page,
            int size,
            CustomUserDetails userDetails
    ) {

        UUID companyUuid = resolveCompanyUuid(userDetails);

        List<UUID> lawyerUuids = accountRepository.findAllByCompanyUuid(companyUuid)
                .stream()
                .map(Account::getUuid)
                .toList();

        Pageable pageable = PageRequest.of(
                page,
                size,
                "OLDEST".equalsIgnoreCase(sort)
                        ? Sort.by("createdAt").ascending()
                        : Sort.by("createdAt").descending()
        );

        Specification<LegalCase> baseSpec = CaseSpecification.build(search, status, type);

        Specification<LegalCase> firmSpec = (root, query, cb) ->
                root.get("lawyerUuid").in(lawyerUuids);

        Specification<LegalCase> finalSpec =
                baseSpec == null ? firmSpec : baseSpec.and(firmSpec);

        Page<LegalCase> casePage = legalCaseRepository.findAll(finalSpec, pageable);

        Set<UUID> accountUuids = casePage.getContent().stream()
                .flatMap(c -> Stream.of(c.getClientUuid(), c.getLawyerUuid()))
                .collect(Collectors.toSet());

        Map<UUID, Account> accounts = accountRepository.findAllByUuidIn(accountUuids)
                .stream()
                .collect(Collectors.toMap(Account::getUuid, Function.identity()));

        List<AdminCaseDto> cases = casePage.getContent().stream().map(c -> {

            AdminCaseDto.AdminCaseDtoBuilder dto = AdminCaseDto.builder();

            dto.caseUuid(c.getUuid());
            dto.caseNumber(c.getCaseNumber());
            dto.title(c.getName());
            dto.caseType(c.getCaseType() != null ? c.getCaseType().getName() : null);
            dto.status(c.getStatus() != null ? c.getStatus().getName() : null);
            dto.createdAt(c.getCreatedAt());

            Account clientAcc = accounts.get(c.getClientUuid());
            if (clientAcc != null) {
                dto.clientName(extractFullName(clientAcc));
                dto.clientProfilePictureUrl(convertGcsUrl(clientAcc.getProfilePictureUrl()));
            }

            Account lawyerAcc = accounts.get(c.getLawyerUuid());
            if (lawyerAcc != null) {
                dto.lawyerName(extractFullName(lawyerAcc));
                dto.lawyerProfilePictureUrl(convertGcsUrl(lawyerAcc.getProfilePictureUrl()));
            }

            return dto.build();

        }).toList();

        return AdminCaseListResponse.builder()
                .content(cases)
                .page(casePage.getNumber())
                .size(casePage.getSize())
                .totalElements(casePage.getTotalElements())
                .totalPages(casePage.getTotalPages())
                .build();
    }

    private UUID resolveCompanyUuid(CustomUserDetails userDetails) {

        Account account = accountRepository.findByUuid(userDetails.getUuid())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (account.getCompanyUuid() == null) {
            throw new RuntimeException("User not linked to company");
        }

        return account.getCompanyUuid();
    }

    public ClientListResponseDto getFirmClients(
            String search,
            String filter,
            int page,
            int size,
            CustomUserDetails userDetails
    ) {

        UUID companyUuid = resolveCompanyUuid(userDetails);

        List<UUID> lawyerUuids = accountRepository.findAllByCompanyUuid(companyUuid)
                .stream()
                .map(Account::getUuid)
                .toList();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Step 1: Get client UUIDs from cases
        List<UUID> clientUuids = legalCaseRepository.findDistinctClientUuidsByLawyerUuidIn(lawyerUuids);

        // Step 2: Fetch client accounts
        Page<Account> clients = accountRepository.findAllByUuidIn(clientUuids, pageable);

        List<AdminClientDto> content = clients.getContent().stream().map(client -> {

            int activeCases = (int) legalCaseRepository
                    .countByClientUuidAndLawyerUuidIn(client.getUuid(), lawyerUuids);

            return AdminClientDto.builder()
                    .uuid(client.getUuid())
                    .name(extractFullName(client))
                    .email(client.getEmail())
                    .phone(extractPhone(client))
                    .activeCases(activeCases)
                    .profilePictureUrl(convertGcsUrl(client.getProfilePictureUrl()))
                    .build();
        }).toList();

        return ClientListResponseDto.builder()
                .content(content)
                .page(clients.getNumber())
                .size(clients.getSize())
                .totalElements(clients.getTotalElements())
                .totalPages(clients.getTotalPages())
                .build();
    }

    public FirmClientsSummaryDto getFirmClientsSummary(CustomUserDetails userDetails) {

        UUID companyUuid = resolveCompanyUuid(userDetails);

        List<UUID> lawyerUuids = accountRepository.findAllByCompanyUuid(companyUuid)
                .stream()
                .map(Account::getUuid)
                .toList();

        List<UUID> clientUuids = legalCaseRepository.findDistinctClientUuidsByLawyerUuidIn(lawyerUuids);

        long totalClients = clientUuids.size();

        long activeCases = legalCaseRepository.countByLawyerUuidIn(lawyerUuids);

        LocalDateTime startOfMonth = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay();

        long newThisMonth = accountRepository
                .countClientsCreatedThisMonth(clientUuids, startOfMonth);

        return FirmClientsSummaryDto.builder()
                .totalClients(totalClients)
                .activeCases(activeCases)
                .newThisMonth(newThisMonth)
                .totalRevenue(0) // placeholder
                .build();
    }

    public FirmCasesSummaryDto getFirmCasesSummary(CustomUserDetails userDetails) {

        Account account = accountRepository.findByUuid(userDetails.getUuid())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UUID companyUuid = account.getCompanyUuid();

        if (companyUuid == null) {
            throw new RuntimeException("User not linked to company");
        }

        List<UUID> lawyerUuids = accountRepository.findAllByCompanyUuid(companyUuid)
                .stream()
                .map(Account::getUuid)
                .toList();

        long total = legalCaseRepository.countByLawyerUuidIn(lawyerUuids);

        long inProgress = legalCaseRepository.countByLawyerUuidInAndStatus_Name(lawyerUuids, "In Progress");

        long completed = legalCaseRepository.countByLawyerUuidInAndStatus_Name(lawyerUuids, "Completed");

        long unassigned = legalCaseRepository.countByLawyerUuidIsNull();

        return FirmCasesSummaryDto.builder()
                .total(total)
                .inProgress(inProgress)
                .completed(completed)
                .unassigned(unassigned)
                .build();
    }

    private String extractPhone(Account account) {

        if (account == null || account.getContactInformation() == null) {
            return "";
        }

        JsonNode contact = account.getContactInformation();

        // adjust key based on your JSON structure
        if (contact.hasNonNull("phone")) {
            return contact.get("phone").asText();
        }

        if (contact.hasNonNull("mobile")) {
            return contact.get("mobile").asText();
        }

        return "";
    }

    public FirmSummaryDto getFirmDashboardSummary(CustomUserDetails userDetails) {
        UUID companyUuid = resolveCompanyUuid(userDetails);

        List<UUID> lawyerUuids = accountRepository.findAllByCompanyUuid(companyUuid)
                .stream()
                .map(Account::getUuid)
                .toList();

        long totalLawyers = lawyerUuids.size();
        long activeCases = legalCaseRepository.countActiveCasesByLawyerUuids(lawyerUuids);
        long totalClients = legalCaseRepository.countDistinctClientsByLawyerUuids(lawyerUuids);

        return FirmSummaryDto.builder()
                .totalLawyers(totalLawyers)
                .activeCases(activeCases)
                .totalClients(totalClients)
                .performance(0.0)
                .build();
    }
}
