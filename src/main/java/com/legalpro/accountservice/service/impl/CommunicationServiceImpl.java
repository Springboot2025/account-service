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
    private static final String GCS_PUBLIC_BASE = "https://storage.googleapis.com/legalpro";

    @Override
    public List<ClientCommunicationSummaryDto> getLawyerCommunications(UUID lawyerUuid, String search) {

        List<Message> messages = messageRepository.findLatestMessagesForLawyer(lawyerUuid);
        List<Quote> quotes = quoteRepository.findLatestQuotesForLawyer(lawyerUuid);
        List<Appointment> appointments = appointmentRepository.findLatestAppointmentsForLawyer(lawyerUuid);

        Map<UUID, ClientCommunicationSummaryDto> summaries = new HashMap<>();

        // Group Messages by Client
        for (Message msg : messages) {
            UUID clientUuid = msg.getSenderUuid().equals(lawyerUuid)
                    ? msg.getReceiverUuid()
                    : msg.getSenderUuid();

            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(clientUuid, k -> {
                ClientCommunicationSummaryDto d = new ClientCommunicationSummaryDto();
                d.setClientUuid(k);
                d.setMessages(new ArrayList<>());
                d.setQuotes(new ArrayList<>());
                d.setAppointments(new ArrayList<>());
                return d;
            });

            dto.getMessages().add(toMessageDto(msg));
        }

        // Group Quotes
        for (Quote q : quotes) {
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(q.getClientUuid(), k -> {
                ClientCommunicationSummaryDto d = new ClientCommunicationSummaryDto();
                d.setClientUuid(k);
                d.setMessages(new ArrayList<>());
                d.setQuotes(new ArrayList<>());
                d.setAppointments(new ArrayList<>());
                return d;
            });

            dto.getQuotes().add(toQuoteDto(q));
        }

        // Group Appointments
        for (Appointment a : appointments) {
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(a.getClientUuid(), k -> {
                ClientCommunicationSummaryDto d = new ClientCommunicationSummaryDto();
                d.setClientUuid(k);
                d.setMessages(new ArrayList<>());
                d.setQuotes(new ArrayList<>());
                d.setAppointments(new ArrayList<>());
                return d;
            });

            dto.getAppointments().add(toAppointmentDto(a));
        }

        // ✅ Bulk Load Accounts
        Set<UUID> ids = new HashSet<>(summaries.keySet());
        ids.add(lawyerUuid);

        Map<UUID, Account> accounts = accountRepository.findByUuidIn(ids)
                .stream().collect(Collectors.toMap(Account::getUuid, a -> a));

        summaries.forEach((clientUuid, dto) -> {
            dto.setLawyerUuid(lawyerUuid);

            Account clientAcc = accounts.get(clientUuid);
            if (clientAcc != null) {
                dto.setClientName(extractNameFromAccount(clientAcc));
                dto.setClientEmail(clientAcc.getEmail());
                dto.setClientProfilePictureUrl(convertGcsUrl(clientAcc.getProfilePictureUrl()));
            }

            Account lawyerAcc = accounts.get(lawyerUuid);
            if (lawyerAcc != null) {
                dto.setLawyerName(extractNameFromAccount(lawyerAcc));
                dto.setLawyerEmail(lawyerAcc.getEmail());
                dto.setLawyerProfilePictureUrl(convertGcsUrl(lawyerAcc.getProfilePictureUrl()));
            }
        });

        return summaries.values().stream()
                .filter(dto -> search == null ||
                        (dto.getClientName() != null && dto.getClientName().toLowerCase().contains(search.toLowerCase())) ||
                        (dto.getClientEmail() != null && dto.getClientEmail().toLowerCase().contains(search.toLowerCase())))
                .toList();
    }


    @Override
    public List<ClientCommunicationSummaryDto> getClientCommunications(UUID clientUuid, String search) {

        List<Message> messages = messageRepository.findAllByClientUuidOrdered(clientUuid);
        List<Quote> quotes = quoteRepository.findByClientUuid(clientUuid);
        List<Appointment> appointments = appointmentRepository.findByClientUuid(clientUuid);

        Map<UUID, ClientCommunicationSummaryDto> summaries = new HashMap<>();

        // Group Messages by Lawyer
        for (Message msg : messages) {
            UUID lawyerUuid = msg.getSenderUuid().equals(clientUuid)
                    ? msg.getReceiverUuid()
                    : msg.getSenderUuid();

            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(lawyerUuid, k -> {
                ClientCommunicationSummaryDto d = new ClientCommunicationSummaryDto();
                d.setLawyerUuid(k);
                d.setMessages(new ArrayList<>());
                d.setQuotes(new ArrayList<>());
                d.setAppointments(new ArrayList<>());
                return d;
            });

            dto.getMessages().add(toMessageDto(msg));
        }

        // Group Quotes
        for (Quote q : quotes) {
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(q.getLawyerUuid(), k -> {
                ClientCommunicationSummaryDto d = new ClientCommunicationSummaryDto();
                d.setLawyerUuid(k);
                d.setMessages(new ArrayList<>());
                d.setQuotes(new ArrayList<>());
                d.setAppointments(new ArrayList<>());
                return d;
            });

            dto.getQuotes().add(toQuoteDto(q));
        }

        // Group Appointments
        for (Appointment a : appointments) {
            ClientCommunicationSummaryDto dto = summaries.computeIfAbsent(a.getLawyerUuid(), k -> {
                ClientCommunicationSummaryDto d = new ClientCommunicationSummaryDto();
                d.setLawyerUuid(k);
                d.setMessages(new ArrayList<>());
                d.setQuotes(new ArrayList<>());
                d.setAppointments(new ArrayList<>());
                return d;
            });

            dto.getAppointments().add(toAppointmentDto(a));
        }

        // ✅ Bulk Load Accounts
        Set<UUID> ids = new HashSet<>(summaries.keySet());
        ids.add(clientUuid);

        Map<UUID, Account> accounts = accountRepository.findByUuidIn(ids)
                .stream().collect(Collectors.toMap(Account::getUuid, a -> a));

        summaries.forEach((lawyerUuid, dto) -> {
            dto.setClientUuid(clientUuid);

            Account clientAcc = accounts.get(clientUuid);
            if (clientAcc != null) {
                dto.setClientName(extractNameFromAccount(clientAcc));
                dto.setClientEmail(clientAcc.getEmail());
            }

            Account lawyerAcc = accounts.get(lawyerUuid);
            if (lawyerAcc != null) {
                dto.setLawyerName(extractNameFromAccount(lawyerAcc));
                dto.setLawyerEmail(lawyerAcc.getEmail());
            }
        });

        return summaries.values().stream()
                .filter(dto -> search == null ||
                        (dto.getLawyerName() != null && dto.getLawyerName().toLowerCase().contains(search.toLowerCase())) ||
                        (dto.getLawyerEmail() != null && dto.getLawyerEmail().toLowerCase().contains(search.toLowerCase())))
                .toList();
    }


    // Extract name from JSON
    private String extractNameFromAccount(Account acc) {
        if (acc.getPersonalDetails() == null) return null;
        var pd = acc.getPersonalDetails();
        if (pd.hasNonNull("fullName")) return pd.get("fullName").asText();
        if (pd.hasNonNull("name")) return pd.get("name").asText();
        if (pd.hasNonNull("firstName") && pd.hasNonNull("lastName"))
            return pd.get("firstName").asText() + " " + pd.get("lastName").asText();
        return null;
    }


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

    private static String convertGcsUrl(String fileUrl) {
        if (fileUrl == null) return null;

        if (fileUrl.startsWith("gs://")) {
            return GCS_PUBLIC_BASE + "/" + fileUrl.substring("gs://".length());
        }
        return fileUrl;
    }
}
