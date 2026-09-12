package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketReplyDto {

    private UUID uuid;
    private UUID senderUuid;
    private String senderName;
    private String senderRole;
    private String message;
    private LocalDateTime createdAt;
    private List<TicketAttachmentDto> attachments;
}
