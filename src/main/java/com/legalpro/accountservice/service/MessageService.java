package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.MessageDto;
import java.util.List;
import java.util.UUID;

public interface MessageService {

    // Send a message from sender → receiver
    MessageDto sendMessage(UUID senderUuid, UUID receiverUuid, String content);

    // Get full conversation between two users
    List<MessageDto> getConversation(UUID user1, UUID user2);

    // Count unread messages for a user
    long countUnread(UUID receiverUuid);

    // Mark all messages as read between two users
    void markAsRead(UUID readerUuid, UUID otherUuid);
}
