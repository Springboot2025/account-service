package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.*;
import com.legalpro.accountservice.dto.admin.*;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.Subscription;
import com.legalpro.accountservice.enums.AdminLawyerStatus;
import com.legalpro.accountservice.enums.AdminSortBy;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.LawyerRatingRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.SubscriptionRepository;
import com.legalpro.accountservice.repository.projection.CaseStatsProjection;
import com.legalpro.accountservice.specification.CaseSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SuperAdminService {
    private final AccountRepository accountRepository;
    private final LegalCaseRepository caseRepository;
    private final LawyerRatingRepository lawyerRatingRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final SubscriptionRepository subscriptionRepository;

    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com/legalpro";
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
                .email(account.getEmail())
                .build();
    }

    public Page<AdminLawyerDto> getLawyers(
            String search,
            AdminLawyerStatus status,
            String category,
            AdminSortBy sort,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));

        AdminLawyerStatus effectiveStatus =
                (status == null || status == AdminLawyerStatus.ALL)
                        ? null
                        : status;

        Page<Account> lawyers =
                accountRepository.findLawyers(
                        search,
                        effectiveStatus != null ? effectiveStatus.name() : null,
                        pageable
                );

        return lawyers.map(lawyer -> {

            CaseStatsProjection stats =
                    caseRepository.getCaseStatsForLawyer(lawyer.getUuid());

            return AdminLawyerDto.from(
                    lawyer,
                    stats
            );
        });
    }


    public DashboardSummaryDto getDashboardSummary() {

        return new DashboardSummaryDto(
                accountRepository.countLawyers(),
                accountRepository.countActiveLawyers(),
                accountRepository.countFirms(),
                accountRepository.countActiveFirms()
        );
    }

    private Sort resolveSort(AdminSortBy sort) {
        return switch (sort) {
            case HIGHEST_RATING -> Sort.by("rating").descending();
            case LOWEST_RATING  -> Sort.by("rating").ascending();
            case ALPHABETICAL  -> Sort.by("created_at").ascending();
            default            -> Sort.by("created_at").descending();
        };
    }

    @Transactional
    public void updateLawyerStatus(UUID lawyerUuid, AdminLawyerStatus status) {

        Account lawyer = accountRepository.findByUuid(lawyerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Lawyer not found"));

        if (status == AdminLawyerStatus.DELETED) {

            if (caseRepository.existsActiveCasesForLawyer(lawyerUuid)) {
                throw new IllegalStateException("Cannot delete lawyer with active cases");
            }

            lawyer.setRemovedAt(LocalDateTime.now());
            lawyer.setActive(false);
        }

        if (status == AdminLawyerStatus.ACTIVE) {
            lawyer.setActive(true);
            lawyer.setRemovedAt(null);
        }

        if (status == AdminLawyerStatus.DEACTIVATED) {
            lawyer.setActive(false);
        }

        accountRepository.save(lawyer);
    }

    private String convertGcsUrl(String fileUrl) {
        if (fileUrl != null && fileUrl.startsWith("gs://")) {
            return GCS_PUBLIC_BASE + "/" + fileUrl.substring("gs://".length());
        }
        return fileUrl;
    }



    public AdminDashboardSummaryDto getAdminDashboardSummary() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startOfThisMonth =
                now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        LocalDateTime startOfLastMonth =
                startOfThisMonth.minusMonths(1);

        LocalDateTime endOfLastMonth =
                startOfThisMonth.minusNanos(1);

        long usersThisMonth =
                accountRepository.countByCreatedAtBetween(startOfThisMonth, now);

        long usersLastMonth =
                accountRepository.countByCreatedAtBetween(startOfLastMonth, endOfLastMonth);

        double usersChangePercent = 0;

        if (usersLastMonth > 0) {
            usersChangePercent =
                    ((double) (usersThisMonth - usersLastMonth) / usersLastMonth) * 100;
        }

        long activeCasesThisMonth =
                caseRepository.countByStatus_NameAndCreatedAtBetween(
                        "Active", startOfThisMonth, now
                );

        long activeCasesLastMonth =
                caseRepository.countByStatus_NameAndCreatedAtBetween(
                        "Active", startOfLastMonth, endOfLastMonth
                );

        long totalUsers = accountRepository.count();

        long activeCases = caseRepository.countByStatus_Name("Active");

        double activeCasesChangePercent = 0;

        if (activeCasesLastMonth > 0) {
            activeCasesChangePercent =
                    ((double) (activeCasesThisMonth - activeCasesLastMonth)
                            / activeCasesLastMonth) * 100;
        }

        return AdminDashboardSummaryDto.builder()
                .totalUsers(totalUsers)
                .usersChangePercent(roundToTwoDecimals(usersChangePercent))
                .activeCases(activeCases)
                .activeCasesChangePercent(roundToTwoDecimals(activeCasesChangePercent))
                .monthlyRevenue(0)              // placeholder
                .revenueChangePercent(0)
                .activeSubscriptions(0)         // placeholder
                .subscriptionsChangePercent(0)
                .build();
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public AdminUserListResponse getUsers(
            String type,
            String search,
            String status,
            String location,
            String sort,
            int page,
            int size
    ) {
        Sort sortOrder;

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

            case "CLIENT":
                accounts = accountRepository.findAll(
                        (root, query, cb) -> cb.equal(
                                root.join("roles").get("name"),
                                "Client"
                        ),
                        pageable
                );
                break;

            case "LAWYER":
                accounts = accountRepository.findAll(
                        (root, query, cb) -> cb.and(
                                cb.equal(root.join("roles").get("name"), "Lawyer"),
                                cb.isFalse(root.get("isCompany"))
                        ),
                        pageable
                );
                break;

            case "FIRM":
                accounts = accountRepository.findAll(
                        (root, query, cb) -> cb.isTrue(root.get("isCompany")),
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
                                    cb.like(cb.lower(root.get("personalDetails").get("firstName")), like),
                                    cb.like(cb.lower(root.get("personalDetails").get("lastName")), like)
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

        String specialization = "";

        if (account.getProfessionalDetails() != null) {
            specialization = account.getProfessionalDetails()
                    .path("specialization")
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
                .cases(cases)
                .rating(rating)
                .spent(0)
                .earned(0)
                .specialization(specialization)
                .joinedAt(joinedAt)
                .lawyerCount(lawyerCount)
                .build();
    }

    public AdminUsersSummaryDto getUsersSummary() {

        long allUsers = accountRepository.count();
        long clients = accountRepository.countByRoleName("Client");
        long lawyers = accountRepository.countByRoleName("Lawyer");
        long firms = accountRepository.countFirms();

        return AdminUsersSummaryDto.builder()
                .allUsers(allUsers)
                .clients(clients)
                .lawyers(lawyers)
                .firms(firms)
                .build();
    }

    public AdminCasesSummaryDto getCasesSummary() {
        long total = legalCaseRepository.count();
        long active = legalCaseRepository.countByStatus_Name("Active");
        long pending = legalCaseRepository.countByStatus_Name("Pending");
        long newCases = legalCaseRepository.countByStatus_Name("New");
        long closed = legalCaseRepository.countByStatus_Name("Closed");
        long urgent = legalCaseRepository.countByStatus_Name("Urgent");
        long won = legalCaseRepository.countByStatus_Name("Won");
        long lost = legalCaseRepository.countByStatus_Name("Lost");

        return AdminCasesSummaryDto.builder()
                .totalCases(total)
                .active(active)
                .pending(pending)
                .newCases(newCases)
                .closed(closed)
                .urgent(urgent)
                .won(won)
                .lost(lost)
                .build();
    }

    public AdminCaseListResponse getCases(
            String search,
            String status,
            String type,
            String sort,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                "OLDEST".equalsIgnoreCase(sort)
                        ? Sort.by("createdAt").ascending()
                        : Sort.by("createdAt").descending()
        );

        Specification<LegalCase> spec = CaseSpecification.build(search, status, type);

        Page<LegalCase> casePage = legalCaseRepository.findAll(spec, pageable);

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
            dto.title(c.getListing());
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

    private String extractFullName(Account account) {
        if (account == null || account.getPersonalDetails() == null) {
            return "Unknown";
        }

        JsonNode pd = account.getPersonalDetails();

        String firstName = pd.hasNonNull("firstName")
                ? pd.get("firstName").asText()
                : "";

        String lastName = pd.hasNonNull("lastName")
                ? pd.get("lastName").asText()
                : "";

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isEmpty() ? "Unknown" : fullName;
    }

    public List<SubscriptionPlanDto> getSubscriptions() {

        List<Subscription> subscriptions =
                subscriptionRepository.findAllByRemovedAtIsNullOrderByIdAsc();

        return subscriptions.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public SubscriptionPlanDto updateSubscription(UUID uuid, UpdateSubscriptionPlanDto dto) {

        Subscription subscription = subscriptionRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (Boolean.TRUE.equals(dto.getRecommended())) {
            List<Subscription> recommendedPlans =
                    subscriptionRepository.findAllByRemovedAtIsNullOrderByIdAsc();

            recommendedPlans.stream()
                    .filter(Subscription::getRecommended)
                    .forEach(plan -> {
                        plan.setRecommended(false);
                        subscriptionRepository.save(plan);
                    });
        }

        subscription.setPlanName(dto.getPlanName());
        subscription.setDescription(dto.getDescription());
        subscription.setMonthlyPrice(dto.getMonthlyPrice());
        subscription.setAnnualPrice(dto.getAnnualPrice());
        subscription.setRecommended(dto.getRecommended());
        subscription.setFeatures(dto.getFeatures());
        subscription.setUpdatedAt(LocalDateTime.now());

        Subscription saved = subscriptionRepository.save(subscription);

        return mapToDto(saved);
    }

    private SubscriptionPlanDto mapToDto(Subscription subscription) {

        return SubscriptionPlanDto.builder()
                .uuid(subscription.getUuid())
                .planName(subscription.getPlanName())
                .description(subscription.getDescription())
                .monthlyPrice(subscription.getMonthlyPrice())
                .annualPrice(subscription.getAnnualPrice())
                .recommended(subscription.getRecommended())
                .features(subscription.getFeatures())
                .build();
    }
}
