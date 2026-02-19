package com.legalpro.accountservice.specification;

import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.LawyerRating;
import com.legalpro.accountservice.entity.Role;
import jakarta.persistence.criteria.*;
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
             * 2️⃣ LOCATION FILTER (Strict match per location)
             * ------------------------------------------------- */
            if (request.getLocations() != null && !request.getLocations().isEmpty()) {

                List<Predicate> locationPredicates = new ArrayList<>();

                for (String location : request.getLocations()) {

                    String[] parts = location.split(",");

                    String suburb = parts.length > 0 ? parts[0].trim() : "";
                    String state = parts.length > 1 ? parts[1].trim() : "";
                    String postcode = parts.length > 2 ? parts[2].trim() : "";

                    List<Predicate> singleLocation = new ArrayList<>();

                    if (!suburb.isBlank()) {
                        singleLocation.add(
                                cb.equal(
                                        cb.lower(cb.function(
                                                "jsonb_extract_path_text",
                                                String.class,
                                                root.get("addressDetails"),
                                                cb.literal("city_suburb")
                                        )),
                                        suburb.toLowerCase()
                                )
                        );
                    }

                    if (!state.isBlank()) {
                        singleLocation.add(
                                cb.equal(
                                        cb.lower(cb.function(
                                                "jsonb_extract_path_text",
                                                String.class,
                                                root.get("addressDetails"),
                                                cb.literal("state_province")
                                        )),
                                        state.toLowerCase()
                                )
                        );
                    }

                    if (!postcode.isBlank()) {
                        singleLocation.add(
                                cb.equal(
                                        cb.function(
                                                "jsonb_extract_path_text",
                                                String.class,
                                                root.get("addressDetails"),
                                                cb.literal("postcode")
                                        ),
                                        postcode
                                )
                        );
                    }

                    locationPredicates.add(
                            cb.and(singleLocation.toArray(new Predicate[0]))
                    );
                }

                predicates.add(cb.or(locationPredicates.toArray(new Predicate[0])));
            }

            /* -------------------------------------------------
             * 3️⃣ NAME FILTER
             * ------------------------------------------------- */
            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {

                String search = "%" + request.getFirstName().toLowerCase() + "%";

                Predicate firstNameMatch = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                root.get("personalDetails"),
                                cb.literal("firstName")
                        )),
                        search
                );

                Predicate lastNameMatch = cb.like(
                        cb.lower(cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                root.get("personalDetails"),
                                cb.literal("lastName")
                        )),
                        search
                );

                predicates.add(cb.or(firstNameMatch, lastNameMatch));
            }

            /* -------------------------------------------------
             * 4️⃣ MOBILE FILTER
             * ------------------------------------------------- */
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
             * 5️⃣ EMAIL FILTER
             * ------------------------------------------------- */
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("email")),
                                "%" + request.getEmail().toLowerCase() + "%"
                        )
                );
            }

            /* -------------------------------------------------
             * 6️⃣ PRACTICE AREA FILTER
             * ------------------------------------------------- */
            if (request.getPracticeArea() != null && !request.getPracticeArea().isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.function(
                                        "jsonb_extract_path_text",
                                        String.class,
                                        root.get("professionalDetails"),
                                        cb.literal("practiceArea")
                                ),
                                request.getPracticeArea()
                        )
                );
            }

            /* -------------------------------------------------
             * 7️⃣ EXPERIENCE RANGE FILTER
             * request.experienceRange:
             *  "1-5", "5-10", "10+"
             * ------------------------------------------------- */
            if (request.getExperienceRange() != null && !request.getExperienceRange().isBlank()) {

                Expression<Integer> experienceExp = cb.function(
                        "to_number",
                        Integer.class,
                        cb.function(
                                "jsonb_extract_path_text",
                                String.class,
                                root.get("professionalDetails"),
                                cb.literal("experienceYears")
                        ),
                        cb.literal("9999")
                );

                switch (request.getExperienceRange()) {

                    case "1-5" ->
                            predicates.add(cb.between(experienceExp, 1, 5));

                    case "5-10" ->
                            predicates.add(cb.between(experienceExp, 5, 10));

                    case "10+" ->
                            predicates.add(cb.greaterThanOrEqualTo(experienceExp, 10));
                }
            }

            /* -------------------------------------------------
             * 8️⃣ RATING FILTER
             * Shows >= selected rating
             * ------------------------------------------------- */
            if (request.getRating() != null) {

                Join<Account, LawyerRating> ratingJoin =
                        root.join("lawyerRatings", JoinType.LEFT);

                Expression<Double> avgRating =
                        cb.avg(ratingJoin.get("rating"));

                // Only apply groupBy & having for main query (not count query)
                if (!Long.class.equals(query.getResultType())) {
                    query.groupBy(root.get("id"));
                    query.having(
                            cb.greaterThanOrEqualTo(
                                    avgRating,
                                    request.getRating().doubleValue()
                            )
                    );
                }
            }

            /* -------------------------------------------------
             * FINAL SAFETY
             * ------------------------------------------------- */
            //query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
