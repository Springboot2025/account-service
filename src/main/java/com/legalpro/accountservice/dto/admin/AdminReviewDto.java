package com.legalpro.accountservice.dto.admin;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewDto {

    private UUID ratingUuid;

    private String lawyerName;

    private String clientName;

    private BigDecimal rating;

    private String review;

    private LocalDateTime createdAt;

    private String status;
}