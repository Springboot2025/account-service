package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lawyer_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawyerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "lawyer_uuid", nullable = false)
    private UUID lawyerUuid;

    @Column(name = "client_uuid", nullable = false)
    private UUID clientUuid;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    private String review;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
