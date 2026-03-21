package com.legalpro.accountservice.dto.admin;

import com.legalpro.accountservice.enums.AccountStatus;
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
    private AccountStatus accountStatus;
    private int cases;
    private double rating;
    private double spent;
    private double earned;
    private String specialization;
    private LocalDateTime joinedAt;
    private int lawyerCount;
}