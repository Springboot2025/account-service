package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDto {
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
    private Map<String, Object> addressDetails;
    private Map<String, Object> contactInformation;
    private Map<String, Object> emergencyContact;


}
