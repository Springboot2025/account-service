package com.legalpro.accountservice.dto;

import com.legalpro.accountservice.entity.TicketCategory;
import com.legalpro.accountservice.entity.TicketPriority;
import com.legalpro.accountservice.entity.TicketStatus;
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
public class SupportTicketDetailDto {

    private UUID uuid;
    private String ticketNumber;
    private String subject;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private TicketCategory category;
    private String createdByName;
    private String createdByRole;
    private UUID createdByUuid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private List<TicketAttachmentDto> attachments;
    private List<TicketReplyDto> replies;
}
