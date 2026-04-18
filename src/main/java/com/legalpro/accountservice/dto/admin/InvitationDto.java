package com.legalpro.accountservice.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class InvitationDto {

    private UUID uuid;
    private String email;
    private String status;

    private LocalDateTime sentAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;

    // placeholders (UI fields)
    private String role;
    private String specialization;
}
