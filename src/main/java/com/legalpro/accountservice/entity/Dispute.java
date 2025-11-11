package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String phone;
    private String organization;
    private String role;

    private String reference;

    @Column(name = "incident_date", nullable = false)
    private LocalDateTime incidentDate;

    @Column(name = "type_of_dispute", nullable = false)
    private String typeOfDispute;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "resolution_requested", columnDefinition = "TEXT", nullable = false)
    private String resolutionRequested;

    @Column(name = "confirm_accuracy", nullable = false)
    private boolean confirmAccuracy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
