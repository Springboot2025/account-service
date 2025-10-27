package com.legalpro.accountservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawyerRatingDto {

    private Long id;
    private UUID uuid;

    private UUID lawyerUuid;
    private UUID clientUuid;

    private BigDecimal rating;
    private String review;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
