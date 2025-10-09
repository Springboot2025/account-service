package com.legalpro.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // matches BIGSERIAL

    @Column(name = "sender_uuid", nullable = false)
    private UUID senderUuid;

    @Column(name = "receiver_uuid", nullable = false)
    private UUID receiverUuid;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "deleted_by_sender", nullable = false)
    @Builder.Default
    private boolean deletedBySender = false;

    @Column(name = "deleted_by_receiver", nullable = false)
    @Builder.Default
    private boolean deletedByReceiver = false;
}
