package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.AppointmentDto;
import com.legalpro.accountservice.entity.Appointment;
import com.legalpro.accountservice.enums.AppointmentStatus;
import com.legalpro.accountservice.mapper.AppointmentMapper;
import com.legalpro.accountservice.repository.AppointmentRepository;
import com.legalpro.accountservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

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
        return appointmentRepository.findByLawyerUuid(lawyerUuid)
                .stream()
                .map(appointmentMapper::toDto)
                .collect(Collectors.toList());
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
}
