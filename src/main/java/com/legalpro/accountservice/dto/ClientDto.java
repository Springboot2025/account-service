package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDto {
    private Long id;
    private UUID uuid;
    private String email;
    private Map<String, Object> personalDetails;
    private Map<String, Object> contactInformation;
    private Map<String, Object> addressDetails;
    private Map<String, Object> preferences;
    private Map<String, Object> emergencyContact;
    private String profilePictureUrl;
    private List<Map<String, Object>> languages;
}
