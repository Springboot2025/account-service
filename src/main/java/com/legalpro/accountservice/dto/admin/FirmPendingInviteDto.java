package com.legalpro.accountservice.dto.admin;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FirmPendingInviteDto {
    private UUID inviteUuid;
    private String email;
    private LocalDateTime invitedAt;
}
