package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminUserDto {
    private UUID uuid;
    private String name;
    private String email;
    private String role;
    private String location;
    private String status;
    private int cases;
    private double rating;
    private double spent;
    private double earned;
}
