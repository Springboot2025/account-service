package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.AppointmentDto;
import com.legalpro.accountservice.dto.ClientCommunicationSummaryDto;
import com.legalpro.accountservice.dto.MessageDto;
import com.legalpro.accountservice.dto.QuoteDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Appointment;
import com.legalpro.accountservice.entity.Message;
import com.legalpro.accountservice.entity.Quote;
import com.legalpro.accountservice.repository.AccountRepository;
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
    private final AccountRepository accountRepository;

    @Override
    public List<ClientCommunicationSummaryDto> getLawyerCommunications(UUID lawyerUuid, String search) {

        log.info("🔍 Fetching communications for lawyer: {}", lawyerUuid);

        List<Message> messages = messageRepository.findAllByUserUuid(lawyerUuid);
        List<Quote> quotes = quoteRepository.findByLawyerUuid(lawyerUuid);
        List<Appointment> appointments = appointmentRepository.findByLawyerUuid(lawyerUuid);

        Map<UUID, ClientCommunicationSummaryDto> summaries = new HashMap<>();

        // Group Messages
        messages.forEach(msg -> {
            UUID clientUuid = msg.getSenderUuid().equals(lawyerUuid)
                    ? msg.getReceiverUuid()
                    : msg.getSenderUuid();

            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(clientUuid, k -> new ClientCommunicationSummaryDto());
            dto.setClientUuid(clientUuid);

            dto.getMessages().add(toMessageDto(msg));
        });

        // Group Quotes
        quotes.forEach(q -> {
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(q.getClientUuid(), k -> new ClientCommunicationSummaryDto());
            dto.setClientUuid(q.getClientUuid());
            dto.getQuotes().add(toQuoteDto(q));
        });

        // Group Appointments
        appointments.forEach(a -> {
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(a.getClientUuid(), k -> new ClientCommunicationSummaryDto());
            dto.setClientUuid(a.getClientUuid());
            dto.getAppointments().add(toAppointmentDto(a));
        });

        // ✅ Hydrate Client & Lawyer details
        summaries.values().forEach(dto -> {
            dto.setLawyerUuid(lawyerUuid);

            accountRepository.findByUuid(dto.getClientUuid()).ifPresent(acc -> {
                dto.setClientName(extractNameFromAccount(acc));
                dto.setClientEmail(acc.getEmail());
            });

            accountRepository.findByUuid(lawyerUuid).ifPresent(acc -> {
                dto.setLawyerName(extractNameFromAccount(acc));
                dto.setLawyerEmail(acc.getEmail());
            });
        });

        return summaries.values().stream()
                .filter(dto -> search == null ||
                        (dto.getClientName() != null && dto.getClientName().toLowerCase().contains(search.toLowerCase())) ||
                        (dto.getClientEmail() != null && dto.getClientEmail().toLowerCase().contains(search.toLowerCase())))
                .sorted(Comparator.comparing(dto ->
                                Optional.ofNullable(dto.getMessages())
                                        .flatMap(list -> list.stream().map(MessageDto::getCreatedAt).max(Comparator.naturalOrder()))
                                        .orElse(null),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientCommunicationSummaryDto> getClientCommunications(UUID clientUuid, String search) {

        log.info("🔍 Fetching communications for client: {}", clientUuid);

        List<Message> messages = messageRepository.findAllByUserUuid(clientUuid);
        List<Quote> quotes = quoteRepository.findByClientUuid(clientUuid);
        List<Appointment> appointments = appointmentRepository.findByClientUuid(clientUuid);

        Map<UUID, ClientCommunicationSummaryDto> summaries = new HashMap<>();

        // Group Messages by Lawyer
        messages.forEach(msg -> {
            UUID lawyerUuid = msg.getSenderUuid().equals(clientUuid)
                    ? msg.getReceiverUuid()
                    : msg.getSenderUuid();

            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(lawyerUuid, k -> new ClientCommunicationSummaryDto());
            dto.setLawyerUuid(lawyerUuid);

            dto.getMessages().add(toMessageDto(msg));
        });

        // Group Quotes
        quotes.forEach(q -> {
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(q.getLawyerUuid(), k -> new ClientCommunicationSummaryDto());
            dto.setLawyerUuid(q.getLawyerUuid());
            dto.getQuotes().add(toQuoteDto(q));
        });

        // Group Appointments
        appointments.forEach(a -> {
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(a.getLawyerUuid(), k -> new ClientCommunicationSummaryDto());
            dto.setLawyerUuid(a.getLawyerUuid());
            dto.getAppointments().add(toAppointmentDto(a));
        });

        // ✅ Hydrate Lawyer & Client details
        summaries.values().forEach(dto -> {
            dto.setClientUuid(clientUuid);

            accountRepository.findByUuid(clientUuid).ifPresent(acc -> {
                dto.setClientName(extractNameFromAccount(acc));
                dto.setClientEmail(acc.getEmail());
            });

            accountRepository.findByUuid(dto.getLawyerUuid()).ifPresent(acc -> {
                dto.setLawyerName(extractNameFromAccount(acc));
                dto.setLawyerEmail(acc.getEmail());
            });
        });

        return summaries.values().stream()
                .filter(dto -> search == null ||
                        (dto.getLawyerName() != null && dto.getLawyerName().toLowerCase().contains(search.toLowerCase())) ||
                        (dto.getLawyerEmail() != null && dto.getLawyerEmail().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());
    }


    // === Helper: Extract Name from personal_details JSON ===
    private String extractNameFromAccount(Account acc) {
        if (acc.getPersonalDetails() == null) return null;

        var pd = acc.getPersonalDetails();

        if (pd.hasNonNull("fullName")) return pd.get("fullName").asText();
        if (pd.hasNonNull("name")) return pd.get("name").asText();
        if (pd.hasNonNull("firstName") && pd.hasNonNull("lastName")) {
            return pd.get("firstName").asText() + " " + pd.get("lastName").asText();
        }

        return null;
    }

    // === DTO Mappers ===

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
