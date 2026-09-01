package com.capitec.capibook.admin;

import com.capitec.capibook.admin.dto.*;
import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.common.ApiResponse;
import com.capitec.capibook.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Validated
@Tag(name = "Admin", description = "Administration endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── User management ──────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> listUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers(pageable)));
    }

    @PostMapping("/users/branch-admins")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create a branch admin account")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createBranchAdmin(
            @Valid @RequestBody CreateBranchAdminRequest request) {

        AdminUserResponse response = adminService.createBranchAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Branch admin created", response));
    }

    @PutMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<ApiResponse<AdminUserResponse>> deactivateUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok("User deactivated", adminService.deactivateUser(userId)));
    }

    // ── Appointments ─────────────────────────────────────────────────────────

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "List appointments (branch-scoped for BRANCH_ADMIN)")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> listAppointments(
            Authentication authentication,
            @RequestParam(required = false) UUID branchId,
            @PageableDefault(size = 20, sort = "appointmentDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<AppointmentResponse> page =
                adminService.listAppointmentsForAdmin(authentication.getName(), branchId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    // ── Branch availability exceptions ────────────────────────────────────────

    @PostMapping("/branches/{branchId}/exceptions")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create a branch availability exception")
    public ResponseEntity<ApiResponse<AvailabilityExceptionResponse>> createException(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateAvailabilityExceptionRequest request) {

        AvailabilityExceptionResponse response = adminService.createException(branchId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Exception created", response));
    }

    @GetMapping("/branches/{branchId}/exceptions")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "List availability exceptions for a branch")
    public ResponseEntity<ApiResponse<List<AvailabilityExceptionResponse>>> listExceptions(
            @PathVariable UUID branchId) {

        return ResponseEntity.ok(ApiResponse.ok(adminService.listExceptions(branchId)));
    }

    @DeleteMapping("/branches/{branchId}/exceptions/{exceptionId}")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Delete a branch availability exception")
    public ResponseEntity<ApiResponse<Void>> deleteException(
            @PathVariable UUID branchId,
            @PathVariable UUID exceptionId) {

        adminService.deleteException(branchId, exceptionId);
        return ResponseEntity.ok(ApiResponse.ok("Exception deleted", null));
    }

    // ── Audit logs ───────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List audit log entries")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> listAuditLogs(
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(adminService.listAuditLogs(pageable)));
    }

    // ── Analytics ────────────────────────────────────────────────────────────

    @GetMapping("/analytics/appointments/summary")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Get appointment summary statistics")
    public ResponseEntity<ApiResponse<AppointmentSummaryResponse>> getAppointmentSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(ApiResponse.ok(adminService.getAppointmentSummary(from, to)));
    }

    @GetMapping("/analytics/branches/{branchId}/utilisation")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Get slot utilisation rate for a branch")
    public ResponseEntity<ApiResponse<BranchUtilisationResponse>> getBranchUtilisation(
            @PathVariable UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(ApiResponse.ok(adminService.getBranchUtilisation(branchId, from, to)));
    }

    @GetMapping("/analytics/services/popularity")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Get most-booked services")
    public ResponseEntity<ApiResponse<List<ServicePopularityResponse>>> getServicePopularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(ApiResponse.ok(adminService.getServicePopularity(from, to)));
    }
}
