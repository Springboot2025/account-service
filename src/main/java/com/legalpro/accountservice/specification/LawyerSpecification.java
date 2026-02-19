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
             * 1️⃣ ONLY LAWYERS
             * ------------------------------------------------- */
            Join<Account, Role> roleJoin = root.join("roles");
            predicates.add(cb.equal(roleJoin.get("name"), "Lawyer"));

            /* -------------------------------------------------
             * 2️⃣ MULTI-LOCATION FILTER (RealEstate Style)
             * ------------------------------------------------- */
            if (request.getLocations() != null && !request.getLocations().isEmpty()) {

                List<Predicate> locationPredicates = new ArrayList<>();

                for (String location : request.getLocations()) {

                    String[] parts = location.split(",");

                    String suburb = parts.length > 0 ? parts[0].trim() : "";
                    String state = parts.length > 1 ? parts[1].trim() : "";
                    String postcode = parts.length > 2 ? parts[2].trim() : "";

                    Predicate suburbMatch = cb.like(
                            cb.lower(
                                    cb.function(
                                            "jsonb_extract_path_text",
                                            String.class,
                                            root.get("addressDetails"),
                                            cb.literal("city_suburb")
                                    )
                            ),
                            "%" + suburb.toLowerCase() + "%"
                    );

                    Predicate stateMatch = cb.like(
                            cb.lower(
                                    cb.function(
                                            "jsonb_extract_path_text",
                                            String.class,
                                            root.get("addressDetails"),
                                            cb.literal("state_province")
                                    )
                            ),
                            "%" + state.toLowerCase() + "%"
                    );

                    Predicate postcodeMatch = cb.like(
                            cb.function(
                                    "jsonb_extract_path_text",
                                    String.class,
                                    root.get("addressDetails"),
                                    cb.literal("postcode")
                            ),
                            "%" + postcode + "%"
                    );

                    // One location must match fully
                    locationPredicates.add(
                            cb.and(suburbMatch, stateMatch, postcodeMatch)
                    );

                }

                // Any location can match
                predicates.add(cb.or(locationPredicates.toArray(new Predicate[0])));
            }

            /* -------------------------------------------------
             * 3️⃣ PERSONAL FILTERS
             * ------------------------------------------------- */

            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {

                String search = "%" + request.getFirstName().toLowerCase() + "%";

                Predicate firstNameMatch = cb.like(
                        cb.lower(
                                cb.function(
                                        "jsonb_extract_path_text",
                                        String.class,
                                        root.get("personalDetails"),
                                        cb.literal("firstName")
                                )
                        ),
                        search
                );

                Predicate lastNameMatch = cb.like(
                        cb.lower(
                                cb.function(
                                        "jsonb_extract_path_text",
                                        String.class,
                                        root.get("personalDetails"),
                                        cb.literal("lastName")
                                )
                        ),
                        search
                );

                predicates.add(cb.or(firstNameMatch, lastNameMatch));
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

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("email")),
                                "%" + request.getEmail().toLowerCase() + "%"
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
