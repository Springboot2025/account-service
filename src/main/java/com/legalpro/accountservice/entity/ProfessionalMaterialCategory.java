package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "professional_material_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalMaterialCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;
}
