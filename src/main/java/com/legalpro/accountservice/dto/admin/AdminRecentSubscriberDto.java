package com.legalpro.accountservice.dto.admin;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRecentSubscriberDto {
    private UUID subscriptionUuid;
    private String name;
    private String email;
    private String userType;
    private String planName;
    private LocalDateTime renewsAt;
    private String status;
}