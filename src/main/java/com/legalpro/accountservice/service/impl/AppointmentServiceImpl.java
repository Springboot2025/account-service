package com.legalpro.accountservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.AppointmentDto;
import com.legalpro.accountservice.dto.AppointmentRequestsSummaryDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.Appointment;
import com.legalpro.accountservice.enums.AppointmentStatus;
import com.legalpro.accountservice.mapper.AppointmentMapper;
import com.legalpro.accountservice.repository.AppointmentRepository;
import com.legalpro.accountservice.service.AppointmentService;
import com.legalpro.accountservice.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final ProfileService profileService;

    // === Client Actions ===

    @Override
    public AppointmentDto bookAppointment(UUID clientUuid, AppointmentDto dto) {
        if (dto.getLawyerUuid() == null) {
            throw new IllegalArgumentException("Lawyer UUID is required for booking an appointment");
        }

        Appointment entity = appointmentMapper.toEntity(dto);
        entity.setUuid(UUID.randomUUID());
        entity.setClientUuid(clientUuid);
        entity.setStatus(AppointmentStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(entity);
        log.info("📅 Appointment booked by client {} with lawyer {} on {} at {}",
                clientUuid, entity.getLawyerUuid(), entity.getAppointmentDate(), entity.getAppointmentTime());

        return appointmentMapper.toDto(saved);
    }

    @Override
    public List<AppointmentDto> getAppointmentsForClient(UUID clientUuid) {
        return appointmentRepository.findByClientUuid(clientUuid)
                .stream()
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentDto getAppointmentForClient(UUID clientUuid, UUID appointmentUuid) {
        Appointment entity = appointmentRepository.findByUuid(appointmentUuid)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!entity.getClientUuid().equals(clientUuid)) {
            throw new RuntimeException("Access denied: not your appointment");
        }
        return appointmentMapper.toDto(entity);
    }


    // === Lawyer Actions ===

    @Override
    public List<AppointmentDto> getAppointmentsForLawyer(UUID lawyerUuid) {

        List<Appointment> appointments = appointmentRepository.findByLawyerUuid(lawyerUuid);

        if (appointments.isEmpty()) return List.of();

        // 1️⃣ Collect client UUIDs
        Set<UUID> clientUuids = appointments.stream()
                .map(Appointment::getClientUuid)
                .collect(Collectors.toSet());

        // 2️⃣ Load all accounts in one DB query
        Map<UUID, Account> accounts = profileService.loadAccounts(clientUuids);

        // 3️⃣ Map + inject clientName
        return appointments.stream().map(a -> {
            AppointmentDto dto = appointmentMapper.toDto(a);

            Account clientAcc = accounts.get(a.getClientUuid());
            if (clientAcc != null) {
                dto.setClientName(extractFullName(clientAcc));
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public AppointmentDto getAppointmentForLawyer(UUID lawyerUuid, UUID appointmentUuid) {
        Appointment entity = appointmentRepository.findByUuid(appointmentUuid)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!entity.getLawyerUuid().equals(lawyerUuid)) {
            throw new RuntimeException("Access denied: not your appointment");
        }
        return appointmentMapper.toDto(entity);
    }

    @Override
    public AppointmentDto updateAppointmentStatus(UUID lawyerUuid, UUID appointmentUuid, AppointmentStatus newStatus, String remarks) {
        Appointment entity = appointmentRepository.findByUuid(appointmentUuid)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!entity.getLawyerUuid().equals(lawyerUuid)) {
            throw new RuntimeException("Access denied: not your appointment");
        }

        entity.setStatus(newStatus);
        entity.setUpdatedAt(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) {
            entity.setNotes(entity.getNotes() == null ? remarks : entity.getNotes() + "\n" + remarks);
        }

        Appointment saved = appointmentRepository.save(entity);
        log.info("✅ Lawyer {} updated appointment {} to status {}", lawyerUuid, appointmentUuid, newStatus);

        return appointmentMapper.toDto(saved);
    }

    @Override
    public AppointmentDto rescheduleAppointment(UUID lawyerUuid, UUID appointmentUuid, AppointmentDto newScheduleDto) {
        Appointment entity = appointmentRepository.findByUuid(appointmentUuid)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!entity.getLawyerUuid().equals(lawyerUuid)) {
            throw new RuntimeException("Access denied: not your appointment");
        }

        entity.setRescheduledFrom(entity.getUuid());
        entity.setAppointmentDate(newScheduleDto.getAppointmentDate());
        entity.setAppointmentTime(newScheduleDto.getAppointmentTime());
        entity.setDurationMinutes(newScheduleDto.getDurationMinutes());
        entity.setStatus(AppointmentStatus.RESCHEDULED);
        entity.setUpdatedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(entity);
        log.info("♻️ Appointment {} rescheduled by lawyer {}", appointmentUuid, lawyerUuid);

        return appointmentMapper.toDto(saved);
    }

    @Override
    public List<AppointmentDto> getAppointmentsForLawyerAndClient(UUID lawyerUuid, UUID clientUuid) {

        List<Appointment> appointments = appointmentRepository.findByLawyerUuidAndClientUuid(lawyerUuid, clientUuid);

        return appointments.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<AppointmentDto> getAppointmentsForClientAndLawyer(UUID clientUuid, UUID lawyerUuid) {

        List<Appointment> appointments = appointmentRepository.findByClientUuidAndLawyerUuid(clientUuid, lawyerUuid);

        return appointments.stream()
                .map(this::toDto)
                .toList();
    }

    private AppointmentDto toDto(Appointment a) {
        return AppointmentDto.builder()
                .id(a.getId())
                .uuid(a.getUuid())
                .clientUuid(a.getClientUuid())
                .lawyerUuid(a.getLawyerUuid())
                .appointmentDate(a.getAppointmentDate())
                .appointmentTime(a.getAppointmentTime())
                .durationMinutes(a.getDurationMinutes())
                .meetingType(a.getMeetingType())
                .notes(a.getNotes())
                .status(a.getStatus())
                .rescheduledFrom(a.getRescheduledFrom())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .deletedAt(a.getDeletedAt())
                .build();
    }

    @Override
    public AppointmentRequestsSummaryDto getSummary(UUID lawyerUuid) {

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);

        // 1️⃣ Pending appointments
        long pending = appointmentRepository.countByLawyerUuidAndStatus(
                lawyerUuid,
                AppointmentStatus.PENDING
        );

        // 2️⃣ Today’s appointments
        long todayCount = appointmentRepository.countByLawyerUuidAndAppointmentDate(
                lawyerUuid,
                today
        );

        // 3️⃣ Next 7 days (exclude today)
        long upcoming = appointmentRepository
                .findByLawyerUuidAndAppointmentDateBetween(
                        lawyerUuid,
                        today.plusDays(1),
                        sevenDaysLater
                )
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .count();

        // 4️⃣ This month
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

        long thisMonth = appointmentRepository
                .findByLawyerUuidAndAppointmentDateBetween(
                        lawyerUuid,
                        firstDay,
                        lastDay
                )
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .count();

        return AppointmentRequestsSummaryDto.builder()
                .pending(pending)
                .today(todayCount)
                .upcoming(upcoming)
                .thisMonth(thisMonth)
                .build();
    }

    private String extractFullName(Account account) {
        if (account == null || account.getPersonalDetails() == null) return "";

        JsonNode pd = account.getPersonalDetails();
        String first = pd.hasNonNull("firstName") ? pd.get("firstName").asText() : "";
        String last  = pd.hasNonNull("lastName")  ? pd.get("lastName").asText()  : "";

        return (first + " " + last).trim();
    }
}
