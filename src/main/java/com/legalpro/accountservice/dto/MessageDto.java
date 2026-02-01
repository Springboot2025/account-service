package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private Long id;
    private UUID uuid;
    private UUID senderUuid;
    private UUID receiverUuid;
    private String content;
    private Instant createdAt;
    private boolean read;

    private String senderProfilePictureUrl;
    private String receiverProfilePictureUrl;
}
