package com.legalpro.accountservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedbackDto {
    private Long id;
    private UUID uuid;
    private UUID userUuid;
    private Double rating;
    private String review;
    private String name;
    private String profession;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
