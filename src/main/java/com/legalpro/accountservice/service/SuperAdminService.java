package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.AccountDto;
import com.legalpro.accountservice.dto.AdminLawyerDto;
import com.legalpro.accountservice.dto.AdminUserDto;
import com.legalpro.accountservice.dto.DashboardSummaryDto;
import com.legalpro.accountservice.dto.AdminDashboardSummaryDto;
import com.legalpro.accountservice.dto.admin.AdminUserListResponse;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.enums.AdminLawyerStatus;
import com.legalpro.accountservice.enums.AdminSortBy;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.projection.CaseStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final AccountRepository accountRepository;
    private final LegalCaseRepository caseRepository;

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

    private static final String GCS_PUBLIC_BASE =
            "https://storage.googleapis.com";

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

        Pageable pageable = PageRequest.of(page, size);

        Page<Account> accounts = accountRepository.findAll(pageable);

        List<AdminUserDto> users = accounts.getContent().stream()
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

        String role = account.getRoles().stream()
                .findFirst()
                .map(r -> r.getName())
                .orElse("");

        if (Boolean.TRUE.equals(account.isCompany())) {
            role = "Firm";
        }

        String location = "";
        if (account.getAddressDetails() != null) {
            location = account.getAddressDetails().path("state").asText("");
        }

        String status = account.isActive() ? "Active" : "Inactive";

        return AdminUserDto.builder()
                .uuid(account.getUuid())
                .name(name)
                .email(account.getEmail())
                .role(role)
                .location(location)
                .status(status)
                .cases(0)      // placeholder
                .rating(0)     // placeholder
                .spent(0)      // placeholder
                .earned(0)     // placeholder
                .build();
    }
}
