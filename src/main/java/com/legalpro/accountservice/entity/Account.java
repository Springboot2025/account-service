package com.legalpro.accountservice.entity;

import com.legalpro.accountservice.util.JpaJsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
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
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID uuid;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String gender;

    @Column(nullable = false, unique = true)
    private String email;

    private String mobile;

    private String address;

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
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID verificationToken;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean terms = false;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean newsletter = false;

    @Column(length = 250)
    private String organization;

    @Column(length = 25)
    private String experience;

    @Column(name = "office_address", columnDefinition = "TEXT")
    private String officeAddress;

    @Column(name = "team_size", length = 50)
    private String teamSize;

    @Column(length = 250)
    private String languages;

    @Column(name = "address_details", columnDefinition = "jsonb")
    @Convert(converter = JpaJsonConverter.class)
    private Map<String, Object> addressDetails;

    @Column(name = "contact_information", columnDefinition = "jsonb")
    @Convert(converter = JpaJsonConverter.class)
    private Map<String, Object> contactInformation;

    @Column(name = "emergency_contact", columnDefinition = "jsonb")
    @Convert(converter = JpaJsonConverter.class)
    private Map<String, Object> emergencyContact;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}
