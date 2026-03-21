package com.legalpro.accountservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionDto {
    private UUID uuid;
    private UUID userUuid;
    private String userType;
    private Long planId;
    private Integer status;
    private String planDuration;
    private LocalDateTime startDate;
    private LocalDateTime renewsAt;
}