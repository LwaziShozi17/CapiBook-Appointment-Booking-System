package com.capitec.capibook.appointment;

import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.appointment.dto.CreateAppointmentRequest;
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
}
