package com.legalpro.accountservice.dto;

import com.legalpro.accountservice.enums.LetterOfAdviceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** One version of a case's Letter of Advice, for the lawyer's history view. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfAdviceHistoryItemDto {
    private UUID uuid;
    /** 1 for the first letter written for the case, 2 for the next, and so on. */
    private int version;
    private String title;
    private LetterOfAdviceStatus status;
    /** True for the one letter currently being worked on / awaiting the client. */
    private boolean current;
    private LocalDateTime createdAt;
    private LocalDateTime lawyerSignedAt;
    private LocalDateTime sentToClientAt;
    private LocalDateTime clientSignedAt;
    private LocalDateTime supersededAt;
    private long changeRequestCount;
    private long unreadChangeRequestCount;
}
