package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.AccountDto;
import com.legalpro.accountservice.dto.AdminLawyerDto;
import com.legalpro.accountservice.dto.DashboardSummaryDto;
import com.legalpro.accountservice.entity.Account;
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

        Page<Account> lawyers =
                accountRepository.findLawyers(
                        search,
                        status != null ? status.name() : null,
                        pageable
                );

        return lawyers.map(lawyer -> {

            CaseStatsProjection stats =
                    caseRepository.getCaseStatsForLawyer(lawyer.getUuid());

            return AdminLawyerDto.from(lawyer, stats);
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

}
