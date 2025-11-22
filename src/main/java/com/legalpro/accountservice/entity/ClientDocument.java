package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "client_documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_client_lawyer_file",
                        columnNames = {"client_uuid", "lawyer_uuid", "file_name", "deleted_at"}
                ),
                @UniqueConstraint(
                        name = "uq_client_document_type",
                        columnNames = {"client_uuid", "document_type", "deleted_at"} // ✅ one document type per client
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_uuid", nullable = false)
    private UUID clientUuid;

    @Column(name = "lawyer_uuid", nullable = true)
    private UUID lawyerUuid;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType; // ✅ new field

    @Column(name = "case_uuid", nullable = true)
    private UUID caseUuid;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
