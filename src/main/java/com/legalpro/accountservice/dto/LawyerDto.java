package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawyerDto {
    private Long id;
    private UUID uuid;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;
    private String email;
    private String mobile;
    private String address;
    private boolean terms;
    private boolean newsletter;
    private String organization;
    private String experience;
    private String officeAddress;
    private String teamSize;
    private String languages;
}
