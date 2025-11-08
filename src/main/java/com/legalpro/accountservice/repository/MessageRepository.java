package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Full conversation between two users (sorted oldest first)
    @Query("""
        SELECT m FROM Message m
        WHERE 
            (m.senderUuid = :user1 AND m.receiverUuid = :user2)
            OR 
            (m.senderUuid = :user2 AND m.receiverUuid = :user1)
        ORDER BY m.createdAt ASC
    """)
    List<Message> findConversation(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2
    );

    // Paginated conversation (sorted newest first)
    @Query("""
        SELECT m FROM Message m
        WHERE 
            (m.senderUuid = :user1 AND m.receiverUuid = :user2)
            OR 
            (m.senderUuid = :user2 AND m.receiverUuid = :user1)
        ORDER BY m.createdAt DESC
    """)
    Page<Message> findConversationPaged(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2,
            Pageable pageable
    );

    // Inbox view — all messages received by a user
    Page<Message> findByReceiverUuid(UUID receiverUuid, Pageable pageable);

    // Count unread messages
    long countByReceiverUuidAndReadFalse(UUID receiverUuid);

    Optional<Message> findTopBySenderUuidAndReceiverUuidOrReceiverUuidAndSenderUuidOrderByCreatedAtDesc(
            UUID senderUuid1, UUID receiverUuid1, UUID senderUuid2, UUID receiverUuid2
    );

    @Query("""
    SELECT m FROM Message m
    WHERE m.senderUuid = :userUuid OR m.receiverUuid = :userUuid
    ORDER BY m.createdAt DESC
""")
    List<Message> findAllByUserUuid(@Param("userUuid") UUID userUuid);

    // ✅ Lawyer - fetch all messages where lawyer is sender or receiver
    @Query("""
        SELECT m FROM Message m
        WHERE m.senderUuid = :userUuid OR m.receiverUuid = :userUuid
        ORDER BY m.createdAt DESC
    """)
    List<Message> findAllByUserUuidOrdered(UUID userUuid);

    // ✅ Client - fetch all messages where client is sender or receiver
    @Query("""
        SELECT m FROM Message m
        WHERE m.senderUuid = :clientUuid OR m.receiverUuid = :clientUuid
        ORDER BY m.createdAt DESC
    """)
    List<Message> findAllByClientUuidOrdered(UUID clientUuid);

}
