package com.capitec.capibook.appointment;

import com.capitec.capibook.appointment.dto.AppointmentHistoryResponse;
import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.appointment.dto.CreateAppointmentRequest;
import com.capitec.capibook.appointment.dto.LifecycleActionRequest;
import com.capitec.capibook.appointment.dto.RescheduleAppointmentRequest;
import com.capitec.capibook.common.ApiResponse;
import com.capitec.capibook.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Appointment booking and retrieval")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Book a new appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            Authentication authentication,
            @Valid @RequestBody CreateAppointmentRequest request) {

        AppointmentResponse response = appointmentService.createAppointment(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Appointment created", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve an appointment by ID")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(
            Authentication authentication,
            @PathVariable UUID id) {

        AppointmentResponse response = appointmentService.getAppointmentById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Retrieve the authenticated customer's appointments")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getMyAppointments(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "appointmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        PageResponse<AppointmentResponse> page =
                appointmentService.getMyAppointments(authentication.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody(required = false) LifecycleActionRequest request) {

        String reason = request != null ? request.reason() : null;
        AppointmentResponse response = appointmentService.cancelAppointment(id, authentication.getName(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Appointment cancelled", response));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Confirm a pending appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> confirm(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody(required = false) LifecycleActionRequest request) {

        String reason = request != null ? request.reason() : null;
        AppointmentResponse response = appointmentService.confirmAppointment(id, authentication.getName(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Appointment confirmed", response));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Mark an appointment as completed")
    public ResponseEntity<ApiResponse<AppointmentResponse>> complete(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody(required = false) LifecycleActionRequest request) {

        String reason = request != null ? request.reason() : null;
        AppointmentResponse response = appointmentService.completeAppointment(id, authentication.getName(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Appointment completed", response));
    }

    @PatchMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Mark an appointment as no-show")
    public ResponseEntity<ApiResponse<AppointmentResponse>> noShow(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody(required = false) LifecycleActionRequest request) {

        String reason = request != null ? request.reason() : null;
        AppointmentResponse response = appointmentService.markNoShow(id, authentication.getName(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Appointment marked as no-show", response));
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule a confirmed appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reschedule(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {

        AppointmentResponse response = appointmentService.rescheduleAppointment(
                id, authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Appointment rescheduled", response));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Retrieve appointment status change history")
    public ResponseEntity<ApiResponse<List<AppointmentHistoryResponse>>> getHistory(
            Authentication authentication,
            @PathVariable UUID id) {

        List<AppointmentHistoryResponse> history =
                appointmentService.getAppointmentHistory(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
