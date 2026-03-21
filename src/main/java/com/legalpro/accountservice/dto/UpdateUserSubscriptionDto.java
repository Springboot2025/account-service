package com.legalpro.accountservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserSubscriptionDto {
    private Integer status;
    private String planDuration;
    private LocalDateTime renewsAt;
}