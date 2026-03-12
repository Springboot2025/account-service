package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewsSummaryDto {

    private Long totalReviews;

    private BigDecimal averageRating;

    private Integer positivePercentage;

    private Long pendingReview;   // placeholder for future moderation feature

    private Long reviewsThisWeek;
}