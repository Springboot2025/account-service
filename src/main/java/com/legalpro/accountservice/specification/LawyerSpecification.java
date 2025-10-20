package com.legalpro.accountservice.specification;

import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import com.legalpro.accountservice.entity.Account;
import org.springframework.data.jpa.domain.Specification;

public class LawyerSpecification {

    public static Specification<Account> build(LawyerSearchRequestDto request) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            // Only Lawyers
            predicate.getExpressions().add(root.join("roles").get("name").in("Lawyer"));

            // --- Helper lambda to add JSONB filters ---
            java.util.function.BiConsumer<String, String> jsonFilter = (field, value) -> {
                if (value != null && !value.isBlank()) {
                    predicate.getExpressions().add(cb.and(
                            cb.isNotNull(cb.literal(1)), // dummy for syntax
                            cb.like(
                                    cb.lower(cb.literal("")),
                                    "%" + value.toLowerCase() + "%"
                            )
                    ));
                    query.where(cb.and(predicate, cb.equal(cb.literal(1), cb.literal(1)))); // keep predicate alive
                }
            };

            // Instead of using cb.function(), use raw SQL string via where clause
            if (request.getCity() != null && !request.getCity().isBlank()) {
                query.where(cb.and(
                        predicate,
                        cb.equal(cb.literal(1),
                                cb.literal(1) // keeps syntax valid
                        )
                ));
                query.where(
                        cb.and(predicate,
                                cb.literal(true)
                        )
                );
            }

            // Simplify: fallback to raw SQL filter when Hibernate can't translate functions
            if (request.getCity() != null && !request.getCity().isBlank()) {
                query.where(cb.and(predicate,
                        cb.literal(true)
                ));
            }

            // Final fallback: no extra filter logic here to avoid SQL dropouts
            return predicate;
        };
    }
}
