package com.legalpro.accountservice.dto;

import com.legalpro.accountservice.enums.AppointmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {

    private Long id;
    private UUID uuid;

    private UUID clientUuid;
    private UUID lawyerUuid;

    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Integer durationMinutes;

    private String meetingType;
    private String notes;

    private AppointmentStatus status;
    private UUID rescheduledFrom;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
