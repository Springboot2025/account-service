package com.legalpro.accountservice.specification;

import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LegalCase;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CaseSpecification {

    public static Specification<LegalCase> build(String search) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            /* -----------------------------------------
             * JOIN STATUS + CASE TYPE
             * ----------------------------------------- */
            root.fetch("status", JoinType.LEFT);
            root.fetch("caseType", JoinType.LEFT);

            /* -----------------------------------------
             * JOIN CLIENT + LAWYER
             * ----------------------------------------- */
            Join<LegalCase, Account> client =
                    root.join("clientAccount", JoinType.LEFT);

            Join<LegalCase, Account> lawyer =
                    root.join("lawyerAccount", JoinType.LEFT);

            /* -----------------------------------------
             * SEARCH
             * ----------------------------------------- */
            if (search != null && !search.isBlank()) {

                String pattern = "%" + search.toLowerCase() + "%";

                Predicate caseNumber = cb.like(
                        cb.lower(root.get("caseNumber")),
                        pattern
                );

                Predicate clientFirst = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                client.get("personalDetails"),
                                cb.literal("firstName")
                        )),
                        pattern
                );

                Predicate clientLast = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                client.get("personalDetails"),
                                cb.literal("lastName")
                        )),
                        pattern
                );

                Predicate lawyerFirst = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                lawyer.get("personalDetails"),
                                cb.literal("firstName")
                        )),
                        pattern
                );

                Predicate lawyerLast = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                lawyer.get("personalDetails"),
                                cb.literal("lastName")
                        )),
                        pattern
                );

                predicates.add(
                        cb.or(
                                caseNumber,
                                clientFirst,
                                clientLast,
                                lawyerFirst,
                                lawyerLast
                        )
                );
            }

            /* -----------------------------------------
             * DELETED FILTER
             * ----------------------------------------- */
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
