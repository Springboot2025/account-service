package com.legalpro.accountservice.specification;

import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.CaseStatus;
import com.legalpro.accountservice.entity.CaseType;
import com.legalpro.accountservice.entity.LegalCase;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseSpecification {

    public static Specification<LegalCase> build(String search, String status, String type) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            root.fetch("status", JoinType.LEFT);
            root.fetch("caseType", JoinType.LEFT);

            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null && !status.isBlank()) {

                Join<LegalCase, CaseStatus> statusJoin =
                        root.join("status", JoinType.LEFT);

                predicates.add(
                        cb.equal(statusJoin.get("name"), status)
                );
            }

            if (type != null && !type.isBlank()) {

                Join<LegalCase, CaseType> typeJoin =
                        root.join("caseType", JoinType.LEFT);

                predicates.add(
                        cb.equal(typeJoin.get("name"), type)
                );
            }

            if (search != null && !search.isBlank()) {

                String pattern = "%" + search.toLowerCase() + "%";

                Predicate caseNumberMatch = cb.like(
                        cb.lower(root.get("caseNumber")),
                        pattern
                );

                Subquery<UUID> clientSub = query.subquery(UUID.class);
                Root<Account> clientRoot = clientSub.from(Account.class);

                Predicate clientFirst = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                clientRoot.get("personalDetails"),
                                cb.literal("firstName")
                        )),
                        pattern
                );

                Predicate clientLast = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                clientRoot.get("personalDetails"),
                                cb.literal("lastName")
                        )),
                        pattern
                );

                clientSub.select(clientRoot.get("uuid"))
                        .where(cb.or(clientFirst, clientLast));

                Predicate clientMatch =
                        root.get("clientUuid").in(clientSub);

                Subquery<UUID> lawyerSub = query.subquery(UUID.class);
                Root<Account> lawyerRoot = lawyerSub.from(Account.class);

                Predicate lawyerFirst = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                lawyerRoot.get("personalDetails"),
                                cb.literal("firstName")
                        )),
                        pattern
                );

                Predicate lawyerLast = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                lawyerRoot.get("personalDetails"),
                                cb.literal("lastName")
                        )),
                        pattern
                );

                lawyerSub.select(lawyerRoot.get("uuid"))
                        .where(cb.or(lawyerFirst, lawyerLast));

                Predicate lawyerMatch =
                        root.get("lawyerUuid").in(lawyerSub);

                predicates.add(
                        cb.or(caseNumberMatch, clientMatch, lawyerMatch)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
