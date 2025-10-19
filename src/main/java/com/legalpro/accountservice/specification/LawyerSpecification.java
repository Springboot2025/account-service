package com.legalpro.accountservice.specification;

import com.legalpro.accountservice.dto.LawyerSearchRequestDto;
import com.legalpro.accountservice.entity.Account;
import org.springframework.data.jpa.domain.Specification;

public class LawyerSpecification {

    public static Specification<Account> build(LawyerSearchRequestDto request) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            // Only include Lawyer accounts
            predicate.getExpressions().add(cb.isTrue(root.join("roles").get("name").in("Lawyer")));

            // personal_details filters
            if (request.getFirstName() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("personalDetails"), cb.literal("firstName"))),
                        "%" + request.getFirstName().toLowerCase() + "%"
                ));
            }
            if (request.getLastName() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("personalDetails"), cb.literal("lastName"))),
                        "%" + request.getLastName().toLowerCase() + "%"
                ));
            }

            // contact_information filters
            if (request.getMobile() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("contactInformation"), cb.literal("mobile"))),
                        "%" + request.getMobile().toLowerCase() + "%"
                ));
            }
            if (request.getHomePhone() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("contactInformation"), cb.literal("homePhone"))),
                        "%" + request.getHomePhone().toLowerCase() + "%"
                ));
            }

            // address_details filters
            if (request.getCity() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("addressDetails"), cb.literal("city"))),
                        "%" + request.getCity().toLowerCase() + "%"
                ));
            }
            if (request.getState() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("addressDetails"), cb.literal("state"))),
                        "%" + request.getState().toLowerCase() + "%"
                ));
            }
            if (request.getCountry() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("addressDetails"), cb.literal("country"))),
                        "%" + request.getCountry().toLowerCase() + "%"
                ));
            }
            if (request.getPostalCode() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("addressDetails"), cb.literal("postalCode"))),
                        "%" + request.getPostalCode().toLowerCase() + "%"
                ));
            }

            // email filter (column)
            if (request.getEmail() != null) {
                predicate.getExpressions().add(cb.like(
                        cb.lower(root.get("email")), "%" + request.getEmail().toLowerCase() + "%"
                ));
            }

            return predicate;
        };
    }
}
