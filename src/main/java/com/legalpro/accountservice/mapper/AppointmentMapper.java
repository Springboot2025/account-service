package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.AppointmentDto;
import com.legalpro.accountservice.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentDto toDto(Appointment entity) {
        if (entity == null) return null;

        return AppointmentDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .clientUuid(entity.getClientUuid())
                .lawyerUuid(entity.getLawyerUuid())
                .appointmentDate(entity.getAppointmentDate())
                .appointmentTime(entity.getAppointmentTime())
                .durationMinutes(entity.getDurationMinutes())
                .meetingType(entity.getMeetingType())
                .notes(entity.getNotes())
                .status(entity.getStatus())
                .rescheduledFrom(entity.getRescheduledFrom())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public Appointment toEntity(AppointmentDto dto) {
        if (dto == null) return null;

        return Appointment.builder()
                .id(dto.getId())
                .uuid(dto.getUuid())
                .clientUuid(dto.getClientUuid())
                .lawyerUuid(dto.getLawyerUuid())
                .appointmentDate(dto.getAppointmentDate())
                .appointmentTime(dto.getAppointmentTime())
                .durationMinutes(dto.getDurationMinutes())
                .meetingType(dto.getMeetingType())
                .notes(dto.getNotes())
                .status(dto.getStatus())
                .rescheduledFrom(dto.getRescheduledFrom())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .deletedAt(dto.getDeletedAt())
                .build();
    }
}
