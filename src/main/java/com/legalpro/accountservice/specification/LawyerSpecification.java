package com.legalpro.accountservice.specification;

import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Role;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class LawyerSpecification {

    public static Specification<Account> build(LawyerSearchRequestDto request) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            /* -------------------------------------------------
             * 1️⃣ RBAC: ONLY LAWYERS
             * ------------------------------------------------- */
            Join<Account, Role> roleJoin = root.join("roles");
            predicates.add(cb.equal(roleJoin.get("name"), "Lawyer"));

            /* -------------------------------------------------
             * 2️⃣ LOCATION FILTERS (JSON fields)
             * ------------------------------------------------- */
            if (request.getCity() != null && !request.getCity().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(
                                        cb.function(
                                                "jsonb_extract_path_text",
                                                String.class,
                                                root.get("addressDetails"),
                                                cb.literal("city")
                                        )
                                ),
                                "%" + request.getCity().toLowerCase() + "%"
                        )
                );
            }

            if (request.getState() != null && !request.getState().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(
                                        cb.function(
                                                "jsonb_extract_path_text",
                                                String.class,
                                                root.get("addressDetails"),
                                                cb.literal("state")
                                        )
                                ),
                                "%" + request.getState().toLowerCase() + "%"
                        )
                );
            }

            if (request.getCountry() != null && !request.getCountry().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(
                                        cb.function(
                                                "jsonb_extract_path_text",
                                                String.class,
                                                root.get("addressDetails"),
                                                cb.literal("country")
                                        )
                                ),
                                "%" + request.getCountry().toLowerCase() + "%"
                        )
                );
            }

            /* -------------------------------------------------
             * 3️⃣ PERSONAL FILTERS
             * ------------------------------------------------- */
            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(
                                        cb.function(
                                                "jsonb_extract_path_text",
                                                String.class,
                                                root.get("personalDetails"),
                                                cb.literal("firstName")
                                        )
                                ),
                                "%" + request.getFirstName().toLowerCase() + "%"
                        )
                );
            }

            if (request.getMobile() != null && !request.getMobile().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.function(
                                        "jsonb_extract_path_text",
                                        String.class,
                                        root.get("contactInformation"),
                                        cb.literal("mobile")
                                ),
                                "%" + request.getMobile() + "%"
                        )
                );
            }

            /* -------------------------------------------------
             * 4️⃣ QUERY SAFETY
             * ------------------------------------------------- */
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
