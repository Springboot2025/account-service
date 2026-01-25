package com.legalpro.accountservice.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // matches BIGSERIAL

    @Column(nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "verification_token")
    private UUID verificationToken;

    @Column(name = "forgot_password_token")
    private UUID forgotPasswordToken;

    @Column(name = "is_company", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isCompany = false;

    @Column(name = "company_uuid", nullable = true)
    private UUID companyUuid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personal_details", columnDefinition = "jsonb", nullable = true)
    private JsonNode personalDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact_information", columnDefinition = "jsonb", nullable = true)
    private JsonNode contactInformation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address_details", columnDefinition = "jsonb", nullable = true)
    private JsonNode addressDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "jsonb", nullable = true)
    private JsonNode preferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emergency_contact", columnDefinition = "jsonb", nullable = true)
    private JsonNode emergencyContact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "professional_details", columnDefinition = "jsonb", nullable = true)
    private JsonNode professionalDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "education_qualification", columnDefinition = "jsonb", nullable = true)
    private JsonNode educationQualification;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "experience_staff", columnDefinition = "jsonb", nullable = true)
    private JsonNode experienceStaff;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "awards_appreciations", columnDefinition = "jsonb", nullable = true)
    private JsonNode awardsAppreciations;

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    private String profilePictureUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consultation_rates", columnDefinition = "jsonb", nullable = true)
    private JsonNode consultationRates;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages", columnDefinition = "jsonb", nullable = true)
    private JsonNode languages;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}
