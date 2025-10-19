package com.legalpro.accountservice.specification;

import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import com.legalpro.accountservice.entity.Account;
import org.springframework.data.jpa.domain.Specification;

public class LawyerSpecification {

    public static Specification<Account> build(LawyerSearchRequestDto request) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            // --- Filter by role: Lawyer ---
            predicate.getExpressions().add(
                    cb.isTrue(root.join("roles").get("name").in("Lawyer"))
            );

            // --- Personal details filters ---
            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                var firstNameExpr = cb.function("jsonb_extract_path_text", String.class,
                        root.get("personalDetails"), cb.literal("firstName"));
                predicate.getExpressions().add(cb.and(
                        cb.isNotNull(firstNameExpr),
                        cb.like(cb.lower(firstNameExpr), "%" + request.getFirstName().toLowerCase() + "%")
                ));
            }

            if (request.getLastName() != null && !request.getLastName().isBlank()) {
                var lastNameExpr = cb.function("jsonb_extract_path_text", String.class,
                        root.get("personalDetails"), cb.literal("lastName"));
                predicate.getExpressions().add(cb.and(
                        cb.isNotNull(lastNameExpr),
                        cb.like(cb.lower(lastNameExpr), "%" + request.getLastName().toLowerCase() + "%")
                ));
            }

            // --- Contact information filters ---
            if (request.getMobile() != null && !request.getMobile().isBlank()) {
                var mobileExpr = cb.function("jsonb_extract_path_text", String.class,
                        root.get("contactInformation"), cb.literal("mobile"));
                predicate.getExpressions().add(cb.and(
                        cb.isNotNull(mobileExpr),
                        cb.like(cb.lower(mobileExpr), "%" + request.getMobile().toLowerCase() + "%")
                ));
            }

            if (request.getHomePhone() != null && !request.getHomePhone().isBlank()) {
                var homePhoneExpr = cb.function("jsonb_extract_path_text", String.class,
                        root.get("contactInformation"), cb.literal("homePhone"));
                predicate.getExpressions().add(cb.and(
                        cb.isNotNull(homePhoneExpr),
                        cb.like(cb.lower(homePhoneExpr), "%" + request.getHomePhone().toLowerCase() + "%")
                ));
            }

            // --- Address filters ---
            if (request.getCity() != null && !request.getCity().isBlank()) {
                var cityExpr = cb.function("jsonb_extract_path_text", String.class,
                        root.get("addressDetails"), cb.literal("city"));
                predicate.getExpressions().add(cb.and(
                        cb.isNotNull(cityExpr),
                        cb.like(cb.lower(cityExpr), "%" + request.getCity().toLowerCase() + "%")
                ));
            }

            if (request.getState() != null && !request.getState().isBlank()) {
                var stateExpr = cb.function("jsonb_extract_path_text", String.class,
                        root.get("addressDetails"), cb.literal("state"));
                predicate.getExpressions().add(cb.and(
                        cb.isNotNull(stateExpr),
                        cb.like(cb.lower(stateExpr), "%" + request.getState().toLowerCase() + "%")
                ));
            }

            if (request.getCountry() != null && !request.getCountry().isBlank()) {
                var countryExpr = cb.function("jsonb_extract_path_text", String.class,
                        root.get("addressDetails"), cb.literal("country"));
                predicate.getExpressions().add(cb.and(
                        cb.isNotNull(countryExpr),
                        cb.like(cb.lower(countryExpr), "%" + request.getCountry().toLowerCase() + "%")
                ));
            }

            if (request.getPostalCode() != null && !request.getPostalCode().isBlank()) {
                var postalExpr = cb.function("jsonb_extract_path_text", String.class,
                        root.get("addressDetails"), cb.literal("postalCode"));
                predicate.getExpressions().add(cb.and(
                        cb.isNotNull(postalExpr),
                        cb.like(cb.lower(postalExpr), "%" + request.getPostalCode().toLowerCase() + "%")
                ));
            }

            // --- Email filter (direct column) ---
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                predicate.getExpressions().add(
                        cb.like(cb.lower(root.get("email")), "%" + request.getEmail().toLowerCase() + "%")
                );
            }

            return predicate;
        };
    }
}
