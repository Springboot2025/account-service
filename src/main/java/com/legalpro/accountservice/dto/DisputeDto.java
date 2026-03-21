package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeDto {

    private Long id;
    private UUID uuid;

    private String fullName;
    private String email;
    private String phone;
    private String organization;
    private String role;

    private String reference;
    private LocalDate incidentDate;
    private String typeOfDispute;
    private String description;
    private String resolutionRequested;
    private boolean confirmAccuracy;
    private Integer documentCount;

    private LocalDateTime createdAt;
}
