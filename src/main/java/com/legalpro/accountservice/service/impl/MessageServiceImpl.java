package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.MessageDto;
import com.legalpro.accountservice.entity.Message;
import com.legalpro.accountservice.repository.MessageRepository;
import com.legalpro.accountservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    @Override
    public MessageDto sendMessage(UUID senderUuid, UUID receiverUuid, String content) {
        Message message = Message.builder()
                .senderUuid(senderUuid)
                .receiverUuid(receiverUuid)
                .content(content)
                .createdAt(Instant.now())
                .read(false)
                .build();

        Message saved = messageRepository.save(message);
        return toDto(saved);
    }

    @Override
    public List<MessageDto> getConversation(UUID user1, UUID user2) {
        return messageRepository.findConversation(user1, user2)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public long countUnread(UUID receiverUuid) {
        return messageRepository.countByReceiverUuidAndReadFalse(receiverUuid);
    }

    @Override
    public void markAsRead(UUID readerUuid, UUID otherUuid) {
        List<Message> conversation = messageRepository.findConversation(readerUuid, otherUuid);
        conversation.stream()
                .filter(msg -> msg.getReceiverUuid().equals(readerUuid) && !msg.isRead())
                .forEach(msg -> msg.setRead(true));
        messageRepository.saveAll(conversation);
    }

    // ===== Helper mapper =====
    private MessageDto toDto(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .senderUuid(message.getSenderUuid())
                .receiverUuid(message.getReceiverUuid())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .read(message.isRead())
                .build();
    }
}
