package com.legalpro.accountservice.dto.admin;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {

    private UUID uuid;

    private String name;

    private String email;

    private String role;       // Client / Lawyer / Firm

    private String location;

    private String status;     // Active / Inactive / Pending

    private int cases;

    private double rating;

    // placeholders for future billing data
    private double spent;      // for clients
    private double earned;     // for lawyers
}