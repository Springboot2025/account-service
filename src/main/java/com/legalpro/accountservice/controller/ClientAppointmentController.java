package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.AppointmentDto;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/client/appointments")
@PreAuthorize("hasRole('Client')")
@RequiredArgsConstructor
public class ClientAppointmentController {

    private final AppointmentService appointmentService;

    // === Book appointment ===
    @PostMapping("/book")
    public ResponseEntity<ApiResponse<AppointmentDto>> bookAppointment(
            @RequestBody AppointmentDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        AppointmentDto booked = appointmentService.bookAppointment(clientUuid, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Appointment booked successfully", booked));
    }

    // === Get all client appointments ===
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getClientAppointments(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        List<AppointmentDto> appointments = appointmentService.getAppointmentsForClient(clientUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Appointments fetched successfully", appointments));
    }

    // === Get single appointment ===
    @GetMapping("/{appointmentUuid}")
    public ResponseEntity<ApiResponse<AppointmentDto>> getClientAppointment(
            @PathVariable UUID appointmentUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientUuid = userDetails.getUuid();
        AppointmentDto appointment = appointmentService.getAppointmentForClient(clientUuid, appointmentUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Appointment details fetched successfully", appointment));
    }
}
