package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDto {
    private Long id;
    private UUID uuid;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String mobile;
    private String address;

    private LocalDate dateOfBirth;
    private boolean terms;
    private boolean newsletter;

    // Lawyer-only fields
    private String organization;
    private String experience;
    private String officeAddress;
    private String teamSize;
    private String languages;
}
