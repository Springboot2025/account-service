package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.AppointmentDto;
import com.legalpro.accountservice.dto.AppointmentRequestsSummaryDto;
import com.legalpro.accountservice.enums.AppointmentStatus;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.AppointmentService;
import lombok.Data;
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
@RequestMapping("/api/lawyer/appointments")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LawyerAppointmentController {

    private final AppointmentService appointmentService;

    // === Get all appointments ===
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointmentsForLawyer(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        List<AppointmentDto> appointments = appointmentService.getAppointmentsForLawyer(lawyerUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Appointments fetched successfully", appointments));
    }

    // === Get a specific appointment ===
    @GetMapping("/{appointmentUuid}")
    public ResponseEntity<ApiResponse<AppointmentDto>> getAppointmentForLawyer(
            @PathVariable UUID appointmentUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        AppointmentDto appointment = appointmentService.getAppointmentForLawyer(lawyerUuid, appointmentUuid);
        return ResponseEntity.ok(ApiResponse.success(200, "Appointment details fetched successfully", appointment));
    }

    // === Unified update endpoint ===
    @PutMapping("/{appointmentUuid}")
    public ResponseEntity<ApiResponse<AppointmentDto>> updateAppointment(
            @PathVariable UUID appointmentUuid,
            @RequestBody AppointmentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        AppointmentDto updated;

        // Handle status change
        if (request.getStatus() != null && request.getStatus() != AppointmentStatus.RESCHEDULED) {
            updated = appointmentService.updateAppointmentStatus(
                    lawyerUuid,
                    appointmentUuid,
                    request.getStatus(),
                    request.getRemarks()
            );
        }
        // Handle reschedule
        else if (request.getStatus() == AppointmentStatus.RESCHEDULED) {
            AppointmentDto dto = new AppointmentDto();
            dto.setAppointmentDate(request.getAppointmentDate());
            dto.setAppointmentTime(request.getAppointmentTime());
            dto.setDurationMinutes(request.getDurationMinutes());
            updated = appointmentService.rescheduleAppointment(lawyerUuid, appointmentUuid, dto);
        } else {
            throw new IllegalArgumentException("Invalid update request — must include status or reschedule info");
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(200, "Appointment updated successfully", updated));
    }

    // === Inner DTO for flexible updates ===
    @Data
    public static class AppointmentUpdateRequest {
        private AppointmentStatus status;
        private String remarks;

        private java.time.LocalDate appointmentDate;
        private java.time.LocalTime appointmentTime;
        private Integer durationMinutes;
    }

    // === Get all appointments between this lawyer and a specific client ===
    @GetMapping("/client/{clientUuid}")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointmentsForClient(
            @PathVariable UUID clientUuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();

        List<AppointmentDto> appointments = appointmentService.getAppointmentsForLawyerAndClient(lawyerUuid, clientUuid);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Appointments for this client fetched successfully", appointments)
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AppointmentRequestsSummaryDto>> getAppointmentSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        AppointmentRequestsSummaryDto summary = appointmentService.getSummary(lawyerUuid);
        return ResponseEntity.ok(
                ApiResponse.success(200, "Appointment summary fetched successfully", summary)
        );
    }

}
