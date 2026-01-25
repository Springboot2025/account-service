package com.legalpro.accountservice.service;

import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AccountRepository accountRepository;

    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com/legalpro";

    /**
     * Bulk-load Account entities for given UUIDs
     */
    public Map<UUID, Account> loadAccounts(Set<UUID> uuids) {
        return accountRepository.findByUuidIn(uuids)
                .stream()
                .collect(Collectors.toMap(Account::getUuid, a -> a));
    }

    /**
     * Convert gs:// URLs to public HTTPS URLs
     */
    public String getProfilePicture(Account account) {
        if (account == null) return null;

        String url = account.getProfilePictureUrl();
        return convertGcsUrl(url);
    }

    /**
     * GCS URL converter
     */
    public String convertGcsUrl(String fileUrl) {
        if (fileUrl == null) return null;

        if (fileUrl.startsWith("gs://")) {
            return GCS_PUBLIC_BASE + "/" + fileUrl.substring("gs://".length());
        }
        return fileUrl;
    }
}
