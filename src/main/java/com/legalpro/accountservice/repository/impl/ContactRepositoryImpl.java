package com.legalpro.accountservice.repository.impl;

import com.legalpro.accountservice.dto.ContactSummaryDto;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.repository.ContactRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ContactRepositoryImpl implements ContactRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<ContactSummaryDto> findContactsForLawyer(UUID lawyerUuid, String search, String filter, Pageable pageable) {
        String baseQuery = """
            SELECT c FROM LegalCase c
            WHERE c.lawyerUuid = :lawyerUuid
              AND c.deletedAt IS NULL
        """;

        if (search != null && !search.isBlank()) {
            baseQuery += " AND (LOWER(c.caseNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.listing) LIKE LOWER(CONCAT('%', :search, '%')))";
        }

        TypedQuery<LegalCase> query = em.createQuery(baseQuery, LegalCase.class);
        query.setParameter("lawyerUuid", lawyerUuid);
        if (search != null && !search.isBlank()) query.setParameter("search", search);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<LegalCase> results = query.getResultList();

        List<ContactSummaryDto> dtos = results.stream().map(c -> {
            // get latest message between lawyer and client
            var msgQuery = em.createQuery("""
                SELECT MAX(m.createdAt)
                FROM Message m
                WHERE (m.senderUuid = :clientUuid AND m.receiverUuid = :lawyerUuid)
                   OR (m.senderUuid = :lawyerUuid AND m.receiverUuid = :clientUuid)
            """, java.time.Instant.class);
            msgQuery.setParameter("lawyerUuid", lawyerUuid);
            msgQuery.setParameter("clientUuid", c.getClientUuid());
            var lastContact = msgQuery.getSingleResult();

            // extract client name and contact info (simplified for now)
            String name = "Client";
            String contactInfo = null;
            try {
                var acc = em.createQuery("SELECT a.contactInformation FROM Account a WHERE a.uuid = :uuid", String.class)
                        .setParameter("uuid", c.getClientUuid())
                        .getSingleResult();
                contactInfo = acc;
            } catch (Exception ignored) {}

            return ContactSummaryDto.builder()
                    .clientUuid(c.getClientUuid())
                    .contactName(name)
                    .caseNumber(c.getCaseNumber())
                    .caseStatus(c.getStatus() != null ? c.getStatus().getName() : null)
                    .contactInfo(contactInfo)
                    .lastContactDate(lastContact)
                    .reminder(c.getFollowUp())
                    .build();
        }).collect(Collectors.toList());

        long total = results.size(); // simplify pagination count for now
        return new PageImpl<>(dtos, pageable, total);
    }
}
