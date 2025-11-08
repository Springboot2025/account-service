package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.AppointmentDto;
import com.legalpro.accountservice.enums.AppointmentStatus;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    // === Client Actions ===

    /**
     * Client books a new appointment with a lawyer.
     */
    AppointmentDto bookAppointment(UUID clientUuid, AppointmentDto dto);

    /**
     * Client views all their appointments.
     */
    List<AppointmentDto> getAppointmentsForClient(UUID clientUuid);

    /**
     * Get a specific appointment for a client.
     */
    AppointmentDto getAppointmentForClient(UUID clientUuid, UUID appointmentUuid);


    // === Lawyer Actions ===

    /**
     * Lawyer views all appointments booked with them.
     */
    List<AppointmentDto> getAppointmentsForLawyer(UUID lawyerUuid);

    /**
     * Get a specific appointment for a lawyer.
     */
    AppointmentDto getAppointmentForLawyer(UUID lawyerUuid, UUID appointmentUuid);

    /**
     * Lawyer updates appointment status (Confirm, Cancel, Complete, etc.).
     */
    AppointmentDto updateAppointmentStatus(UUID lawyerUuid, UUID appointmentUuid, AppointmentStatus newStatus, String remarks);

    /**
     * Lawyer reschedules an existing appointment.
     */
    AppointmentDto rescheduleAppointment(UUID lawyerUuid, UUID appointmentUuid, AppointmentDto newScheduleDto);

    List<AppointmentDto> getAppointmentsForLawyerAndClient(UUID lawyerUuid, UUID clientUuid);

    List<AppointmentDto> getAppointmentsForClientAndLawyer(UUID clientUuid, UUID lawyerUuid);

}
