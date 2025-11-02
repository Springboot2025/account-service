package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.AppointmentDto;
import com.legalpro.accountservice.dto.ClientCommunicationSummaryDto;
import com.legalpro.accountservice.dto.MessageDto;
import com.legalpro.accountservice.dto.QuoteDto;
import com.legalpro.accountservice.entity.Appointment;
import com.legalpro.accountservice.entity.Message;
import com.legalpro.accountservice.entity.Quote;
import com.legalpro.accountservice.repository.AppointmentRepository;
import com.legalpro.accountservice.repository.MessageRepository;
import com.legalpro.accountservice.repository.QuoteRepository;
import com.legalpro.accountservice.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationServiceImpl implements CommunicationService {

    private final MessageRepository messageRepository;
    private final QuoteRepository quoteRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public List<ClientCommunicationSummaryDto> getLawyerCommunications(UUID lawyerUuid, String search) {

        log.info("🔍 Fetching communications for lawyer: {}", lawyerUuid);

        // === Step 1: Fetch data for the lawyer ===
        List<Message> messages = messageRepository.findAllByUserUuid(lawyerUuid);
        List<Quote> quotes = quoteRepository.findByLawyerUuid(lawyerUuid);
        List<Appointment> appointments = appointmentRepository.findByLawyerUuid(lawyerUuid);

        // === Step 2: Group data by client UUID ===
        Map<UUID, ClientCommunicationSummaryDto> summaries = new HashMap<>();

        // --- Group Messages ---
        messages.forEach(msg -> {
            UUID clientUuid = msg.getSenderUuid().equals(lawyerUuid)
                    ? msg.getReceiverUuid()
                    : msg.getSenderUuid();

            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(clientUuid, k -> new ClientCommunicationSummaryDto());
            dto.setClientUuid(clientUuid);

            if (dto.getMessages() == null)
                dto.setMessages(new ArrayList<>());

            dto.getMessages().add(toMessageDto(msg));
        });

        // --- Group Quotes ---
        quotes.forEach(q -> {
            UUID clientUuid = q.getClientUuid();
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(clientUuid, k -> new ClientCommunicationSummaryDto());
            dto.setClientUuid(clientUuid);

            if (dto.getQuotes() == null)
                dto.setQuotes(new ArrayList<>());

            dto.getQuotes().add(toQuoteDto(q));
        });

        // --- Group Appointments ---
        appointments.forEach(a -> {
            UUID clientUuid = a.getClientUuid();
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(clientUuid, k -> new ClientCommunicationSummaryDto());
            dto.setClientUuid(clientUuid);

            if (dto.getAppointments() == null)
                dto.setAppointments(new ArrayList<>());

            dto.getAppointments().add(toAppointmentDto(a));
        });

        // === Step 3: Optional search filter ===
        return summaries.values().stream()
                .filter(dto ->
                        search == null ||
                                (dto.getClientName() != null && dto.getClientName().toLowerCase().contains(search.toLowerCase())) ||
                                (dto.getClientEmail() != null && dto.getClientEmail().toLowerCase().contains(search.toLowerCase()))
                )
                .sorted(Comparator.comparing(dto ->
                                Optional.ofNullable(dto.getMessages())
                                        .flatMap(list -> list.stream()
                                                .map(MessageDto::getCreatedAt)
                                                .max(Comparator.naturalOrder()))
                                        .orElse(null),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // === Helper Mappers ===

    private MessageDto toMessageDto(Message m) {
        return MessageDto.builder()
                .id(m.getId())
                .uuid(m.getUuid())
                .senderUuid(m.getSenderUuid())
                .receiverUuid(m.getReceiverUuid())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private QuoteDto toQuoteDto(Quote q) {
        return QuoteDto.builder()
                .id(q.getId())
                .uuid(q.getUuid())
                .title(q.getTitle())
                .description(q.getDescription())
                .expectedAmount(q.getExpectedAmount())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .build();
    }

    private AppointmentDto toAppointmentDto(Appointment a) {
        return AppointmentDto.builder()
                .id(a.getId())
                .uuid(a.getUuid())
                .appointmentDate(a.getAppointmentDate())
                .appointmentTime(a.getAppointmentTime())
                .durationMinutes(a.getDurationMinutes())
                .status(a.getStatus())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
