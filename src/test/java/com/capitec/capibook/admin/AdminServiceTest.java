package com.capitec.capibook.admin;

import com.capitec.capibook.admin.dto.*;
import com.capitec.capibook.appointment.Appointment;
import com.capitec.capibook.appointment.AppointmentRepository;
import com.capitec.capibook.audit.AuditLog;
import com.capitec.capibook.audit.AuditLogRepository;
import com.capitec.capibook.appointment.AppointmentStatus;
import com.capitec.capibook.audit.AuditLogRepository;
import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchAvailabilityException;
import com.capitec.capibook.branch.BranchAvailabilityExceptionRepository;
import com.capitec.capibook.branch.BranchRepository;
import com.capitec.capibook.common.PageResponse;
import com.capitec.capibook.exception.DuplicateEmailException;
import com.capitec.capibook.exception.DuplicateResourceException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.servicecatalog.BankingService;
import com.capitec.capibook.user.Role;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private BranchAvailabilityExceptionRepository availabilityExceptionRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private UUID branchId;
    private Branch branch;
    private User branchAdmin;
    private User systemAdmin;

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();

        branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", branchId);
        ReflectionTestUtils.setField(branch, "name", "Test Branch");

        branchAdmin = new User();
        ReflectionTestUtils.setField(branchAdmin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(branchAdmin, "email", "admin@test.com");
        ReflectionTestUtils.setField(branchAdmin, "role", Role.BRANCH_ADMIN);
        ReflectionTestUtils.setField(branchAdmin, "firstName", "Branch");
        ReflectionTestUtils.setField(branchAdmin, "lastName", "Admin");
        ReflectionTestUtils.setField(branchAdmin, "branchId", branchId);
        ReflectionTestUtils.setField(branchAdmin, "active", true);

        systemAdmin = new User();
        ReflectionTestUtils.setField(systemAdmin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(systemAdmin, "email", "sysadmin@test.com");
        ReflectionTestUtils.setField(systemAdmin, "role", Role.SYSTEM_ADMIN);
        ReflectionTestUtils.setField(systemAdmin, "firstName", "System");
        ReflectionTestUtils.setField(systemAdmin, "lastName", "Admin");
        ReflectionTestUtils.setField(systemAdmin, "active", true);
    }

    // ── createBranchAdmin ────────────────────────────────────────────────────

    @Test
    void createBranchAdmin_duplicateEmail_throwsDuplicateEmailException() {
        CreateBranchAdminRequest request = new CreateBranchAdminRequest(
                "existing@test.com", "First", "Last", null, branchId, "Pass123!");

        when(userRepository.findByEmail("existing@test.com"))
                .thenReturn(Optional.of(branchAdmin));

        assertThatThrownBy(() -> adminService.createBranchAdmin(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void createBranchAdmin_branchNotFound_throwsResourceNotFoundException() {
        CreateBranchAdminRequest request = new CreateBranchAdminRequest(
                "newadmin@test.com", "First", "Last", null, UUID.randomUUID(), "Pass123!");

        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(branchRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createBranchAdmin(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createBranchAdmin_validRequest_savesAndReturnsUser() {
        UUID newBranchId = UUID.randomUUID();
        Branch newBranch = new Branch();
        ReflectionTestUtils.setField(newBranch, "id", newBranchId);
        ReflectionTestUtils.setField(newBranch, "name", "New Branch");

        CreateBranchAdminRequest request = new CreateBranchAdminRequest(
                "newadmin@test.com", "New", "Admin", "0821234567", newBranchId, "Pass123!");

        when(userRepository.findByEmail("newadmin@test.com")).thenReturn(Optional.empty());
        when(branchRepository.findById(newBranchId)).thenReturn(Optional.of(newBranch));
        when(passwordEncoder.encode("Pass123!")).thenReturn("$hashed$");

        User savedUser = new User();
        ReflectionTestUtils.setField(savedUser, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(savedUser, "email", "newadmin@test.com");
        ReflectionTestUtils.setField(savedUser, "role", Role.BRANCH_ADMIN);
        ReflectionTestUtils.setField(savedUser, "firstName", "New");
        ReflectionTestUtils.setField(savedUser, "lastName", "Admin");
        ReflectionTestUtils.setField(savedUser, "branchId", newBranchId);
        ReflectionTestUtils.setField(savedUser, "active", true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AdminUserResponse response = adminService.createBranchAdmin(request);

        assertThat(response.role()).isEqualTo("BRANCH_ADMIN");
        assertThat(response.email()).isEqualTo("newadmin@test.com");
    }

    // ── listAppointmentsForAdmin ─────────────────────────────────────────────

    @Test
    void listAppointmentsForAdmin_branchAdmin_withNoBranchId_returnsEmpty() {
        User adminNoBranch = new User();
        ReflectionTestUtils.setField(adminNoBranch, "email", "nobranch@test.com");
        ReflectionTestUtils.setField(adminNoBranch, "role", Role.BRANCH_ADMIN);
        ReflectionTestUtils.setField(adminNoBranch, "branchId", null);

        when(userRepository.findByEmail("nobranch@test.com")).thenReturn(Optional.of(adminNoBranch));

        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<?> result = adminService.listAppointmentsForAdmin("nobranch@test.com", null, pageable);

        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    void listAppointmentsForAdmin_branchAdmin_scopedToOwnBranch() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(branchAdmin));

        Appointment appointment = buildMockAppointment();
        Pageable pageable = PageRequest.of(0, 10);
        when(appointmentRepository.findByBranchId(branchId, pageable))
                .thenReturn(new PageImpl<>(List.of(appointment)));

        PageResponse<?> result = adminService.listAppointmentsForAdmin("admin@test.com", null, pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        verify(appointmentRepository).findByBranchId(branchId, pageable);
    }

    @Test
    void listAppointmentsForAdmin_systemAdmin_withBranchFilter_filtersByBranch() {
        when(userRepository.findByEmail("sysadmin@test.com")).thenReturn(Optional.of(systemAdmin));

        UUID filterBranchId = UUID.randomUUID();
        Appointment appointment = buildMockAppointment();
        Pageable pageable = PageRequest.of(0, 10);
        when(appointmentRepository.findByBranchId(filterBranchId, pageable))
                .thenReturn(new PageImpl<>(List.of(appointment)));

        PageResponse<?> result = adminService.listAppointmentsForAdmin("sysadmin@test.com", filterBranchId, pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        verify(appointmentRepository).findByBranchId(filterBranchId, pageable);
    }

    @Test
    void listAppointmentsForAdmin_systemAdmin_noFilter_returnsAll() {
        when(userRepository.findByEmail("sysadmin@test.com")).thenReturn(Optional.of(systemAdmin));

        Appointment appointment = buildMockAppointment();
        Pageable pageable = PageRequest.of(0, 10);
        when(appointmentRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(appointment)));

        PageResponse<?> result = adminService.listAppointmentsForAdmin("sysadmin@test.com", null, pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        verify(appointmentRepository).findAll(pageable);
    }

    // ── createException ──────────────────────────────────────────────────────

    @Test
    void createException_duplicate_throwsDuplicateResourceException() {
        CreateAvailabilityExceptionRequest request = new CreateAvailabilityExceptionRequest(
                LocalDate.now().plusDays(7), com.capitec.capibook.branch.ExceptionType.CLOSED, "Maintenance");

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(availabilityExceptionRepository
                .existsByBranchIdAndExceptionDate(branchId, request.exceptionDate()))
                .thenReturn(true);

        assertThatThrownBy(() -> adminService.createException(branchId, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createException_branchNotFound_throwsResourceNotFoundException() {
        CreateAvailabilityExceptionRequest request = new CreateAvailabilityExceptionRequest(
                LocalDate.now().plusDays(7), com.capitec.capibook.branch.ExceptionType.CLOSED, "Maintenance");

        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createException(branchId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── listExceptions ───────────────────────────────────────────────────────

    @Test
    void listExceptions_branchNotFound_throwsResourceNotFoundException() {
        when(branchRepository.existsById(branchId)).thenReturn(false);

        assertThatThrownBy(() -> adminService.listExceptions(branchId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listExceptions_validBranch_returnsList() {
        when(branchRepository.existsById(branchId)).thenReturn(true);

        BranchAvailabilityException exception = new BranchAvailabilityException();
        exception.setBranch(branch);
        ReflectionTestUtils.setField(exception, "id", UUID.randomUUID());
        exception.setExceptionDate(LocalDate.now().plusDays(7));
        exception.setType(com.capitec.capibook.branch.ExceptionType.CLOSED);
        exception.setReason("Maintenance");

        when(availabilityExceptionRepository.findByBranchIdOrderByExceptionDateAsc(branchId))
                .thenReturn(List.of(exception));

        List<AvailabilityExceptionResponse> result = adminService.listExceptions(branchId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).reason()).isEqualTo("Maintenance");
    }

    // ── deleteException ──────────────────────────────────────────────────────

    @Test
    void deleteException_exceptionNotFound_throwsResourceNotFoundException() {
        UUID exceptionId = UUID.randomUUID();
        when(availabilityExceptionRepository.findById(exceptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteException(branchId, exceptionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteException_exceptionBelongsToWrongBranch_throwsResourceNotFoundException() {
        UUID exceptionId = UUID.randomUUID();
        Branch otherBranch = new Branch();
        ReflectionTestUtils.setField(otherBranch, "id", UUID.randomUUID());

        BranchAvailabilityException exception = new BranchAvailabilityException();
        exception.setBranch(otherBranch);
        ReflectionTestUtils.setField(exception, "id", exceptionId);

        when(availabilityExceptionRepository.findById(exceptionId)).thenReturn(Optional.of(exception));

        assertThatThrownBy(() -> adminService.deleteException(branchId, exceptionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(exceptionId.toString());
    }

    @Test
    void deleteException_validRequest_deletesException() {
        UUID exceptionId = UUID.randomUUID();
        BranchAvailabilityException exception = new BranchAvailabilityException();
        exception.setBranch(branch);
        ReflectionTestUtils.setField(exception, "id", exceptionId);

        when(availabilityExceptionRepository.findById(exceptionId)).thenReturn(Optional.of(exception));

        adminService.deleteException(branchId, exceptionId);

        verify(availabilityExceptionRepository).delete(exception);
    }

    // ── getBranchUtilisation ─────────────────────────────────────────────────

    @Test
    void getBranchUtilisation_branchNotFound_throwsResourceNotFoundException() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getBranchUtilisation(
                branchId, LocalDate.now().minusDays(7), LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBranchUtilisation_validRequest_returnsResponse() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(appointmentRepository.countByBranchAndDateRangeAndStatuses(
                eq(branchId), eq(from), eq(to), any())).thenReturn(5L, 10L);

        BranchUtilisationResponse response = adminService.getBranchUtilisation(branchId, from, to);

        assertThat(response.branchId()).isEqualTo(branchId);
        assertThat(response.branchName()).isEqualTo("Test Branch");
        assertThat(response.periodStart()).isEqualTo(from);
        assertThat(response.periodEnd()).isEqualTo(to);
    }

    @Test
    void getBranchUtilisation_noSlots_returnsZeroUtilisation() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(appointmentRepository.countByBranchAndDateRangeAndStatuses(
                eq(branchId), eq(from), eq(to), any())).thenReturn(0L);

        BranchUtilisationResponse response = adminService.getBranchUtilisation(branchId, from, to);

        assertThat(response.utilisation()).isEqualTo(0.0);
    }

    // ── getServicePopularity ─────────────────────────────────────────────────

    @Test
    void getServicePopularity_returnsRankedList() {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        UUID serviceId = UUID.randomUUID();

        Object[] row = {serviceId, "Card Collection", 25L};
        List<Object[]> rows = java.util.Collections.singletonList(row);
        when(appointmentRepository.findServicePopularity(from, to)).thenReturn(rows);

        List<ServicePopularityResponse> result = adminService.getServicePopularity(from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).serviceId()).isEqualTo(serviceId);
        assertThat(result.get(0).serviceName()).isEqualTo("Card Collection");
        assertThat(result.get(0).totalBookings()).isEqualTo(25L);
    }

    @Test
    void getServicePopularity_noAppointments_returnsEmptyList() {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        when(appointmentRepository.findServicePopularity(from, to))
                .thenReturn(java.util.Collections.emptyList());

        List<ServicePopularityResponse> result = adminService.getServicePopularity(from, to);

        assertThat(result).isEmpty();
    }

    // ── deactivateUser ───────────────────────────────────────────────────────

    @Test
    void deactivateUser_userNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deactivateUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── listAuditLogs ────────────────────────────────────────────────────────

    @Test
    void listAuditLogs_returnsPageWithMappedResponses() {
        AuditLog log = new AuditLog();
        ReflectionTestUtils.setField(log, "id", UUID.randomUUID());
        log.setAction("APPOINTMENT_CREATED");
        log.setEntityType("Appointment");
        log.setEntityId(UUID.randomUUID().toString());
        log.setActorId(UUID.randomUUID());
        log.setDetails("{\"test\":\"data\"}");
        ReflectionTestUtils.setField(log, "createdAt", java.time.LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(log)));

        PageResponse<AuditLogResponse> result = adminService.listAuditLogs(pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).action()).isEqualTo("APPOINTMENT_CREATED");
        assertThat(result.content().get(0).entityType()).isEqualTo("Appointment");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Appointment buildMockAppointment() {
        User customer = new User();
        ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(customer, "firstName", "John");
        ReflectionTestUtils.setField(customer, "lastName", "Doe");

        BankingService service = new BankingService();
        ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(service, "name", "Card Collection");

        Appointment appointment = new Appointment();
        ReflectionTestUtils.setField(appointment, "id", UUID.randomUUID());
        appointment.setCustomer(customer);
        appointment.setBranch(branch);
        appointment.setService(service);
        appointment.setAppointmentDate(LocalDate.now().plusDays(7));
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(9, 15));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReferenceNumber("CAP-2026-ABCDE");
        ReflectionTestUtils.setField(appointment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(appointment, "updatedAt", LocalDateTime.now());

        return appointment;
    }
}
