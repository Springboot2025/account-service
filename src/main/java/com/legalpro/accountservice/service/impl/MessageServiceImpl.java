package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.MessageDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Message;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.MessageRepository;
import com.legalpro.accountservice.service.ActivityLogService;
import com.legalpro.accountservice.service.MessageService;
import com.legalpro.accountservice.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ActivityLogService activityLogService;
    private final AccountRepository accountRepository;
    private final ProfileService profileService;

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

        UUID lawyerUuid = null;
        UUID clientUuid = null;

        Account sender = accountRepository.findByUuid(senderUuid)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Account receiver = accountRepository.findByUuid(receiverUuid)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (sender.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("Lawyer"))) {
            lawyerUuid = senderUuid;
        } else {
            clientUuid = senderUuid;
        }

        if (receiver.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("Lawyer"))) {
            lawyerUuid = receiverUuid;
        } else {
            clientUuid = receiverUuid;
        }

        String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;

        activityLogService.logActivity(
                "MESSAGE_SENT",
                "New message sent",
                senderUuid,                 // actorUuid
                lawyerUuid,                 // lawyerUuid (if applicable)
                clientUuid,                 // clientUuid (if applicable)
                null,                       // caseUuid (no case link)
                saved.getUuid(),            // referenceUuid
                Map.of("preview", preview)  // metadata
        );

        return toDto(saved);
    }

    @Override
    public List<MessageDto> getConversation(UUID user1, UUID user2) {

        List<Message> messages = messageRepository.findConversation(user1, user2);

        if (messages.isEmpty()) {
            return List.of();
        }

        // 1️⃣ Collect all sender + receiver UUIDs
        Set<UUID> accountIds = new HashSet<>();
        messages.forEach(m -> {
            accountIds.add(m.getSenderUuid());
            accountIds.add(m.getReceiverUuid());
        });

        // 2️⃣ Load ALL accounts in one go
        Map<UUID, Account> accounts = profileService.loadAccounts(accountIds);

        // 3️⃣ Convert to DTO + inject profile pictures
        return messages.stream().map(m -> {
            MessageDto dto = toDto(m);

            Account senderAcc = accounts.get(m.getSenderUuid());
            dto.setSenderProfilePictureUrl(
                    profileService.getProfilePicture(senderAcc)
            );

            Account receiverAcc = accounts.get(m.getReceiverUuid());
            dto.setReceiverProfilePictureUrl(
                    profileService.getProfilePicture(receiverAcc)
            );

            return dto;
        }).toList();
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
                .uuid(message.getUuid())
                .senderUuid(message.getSenderUuid())
                .receiverUuid(message.getReceiverUuid())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .read(message.isRead())
                .build();
    }
}
