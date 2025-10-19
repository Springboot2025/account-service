package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // BIGSERIAL

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
