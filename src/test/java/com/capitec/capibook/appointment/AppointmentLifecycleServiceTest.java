package com.capitec.capibook.appointment;

import com.capitec.capibook.appointment.dto.AppointmentHistoryResponse;
import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.appointment.dto.RescheduleAppointmentRequest;
import com.capitec.capibook.availability.PublicHolidayRepository;
import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchOperatingHours;
import com.capitec.capibook.branch.BranchOperatingHoursRepository;
import com.capitec.capibook.branch.BranchRepository;
import com.capitec.capibook.exception.InvalidStatusTransitionException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.servicecatalog.BankingService;
import com.capitec.capibook.servicecatalog.BankingServiceRepository;
import com.capitec.capibook.user.Role;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import com.capitec.capibook.metrics.BookingMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentLifecycleServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentHistoryRepository appointmentHistoryRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private BankingServiceRepository bankingServiceRepository;
    @Mock private BranchOperatingHoursRepository operatingHoursRepository;
    @Mock private PublicHolidayRepository publicHolidayRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private BookingMetricsService metricsService;

    @InjectMocks
    private AppointmentService appointmentService;

    private User customer;
    private User branchAdmin;
    private User systemAdmin;
    private Appointment appointment;

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID BRANCH_ADMIN_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final String CUSTOMER_EMAIL = "customer@test.com";
    private static final String BRANCH_ADMIN_EMAIL = "admin@test.com";
    private static final String SYSTEM_ADMIN_EMAIL = "sysadmin@test.com";

    @BeforeEach
    void setUp() {
        customer = new User();
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        ReflectionTestUtils.setField(customer, "email", CUSTOMER_EMAIL);
        ReflectionTestUtils.setField(customer, "role", Role.CUSTOMER);
        ReflectionTestUtils.setField(customer, "firstName", "John");
        ReflectionTestUtils.setField(customer, "lastName", "Doe");

        branchAdmin = new User();
        ReflectionTestUtils.setField(branchAdmin, "id", BRANCH_ADMIN_ID);
        ReflectionTestUtils.setField(branchAdmin, "email", BRANCH_ADMIN_EMAIL);
        ReflectionTestUtils.setField(branchAdmin, "role", Role.BRANCH_ADMIN);
        ReflectionTestUtils.setField(branchAdmin, "firstName", "Admin");
        ReflectionTestUtils.setField(branchAdmin, "lastName", "User");
        ReflectionTestUtils.setField(branchAdmin, "branchId", BRANCH_ID);

        systemAdmin = new User();
        ReflectionTestUtils.setField(systemAdmin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(systemAdmin, "email", SYSTEM_ADMIN_EMAIL);
        ReflectionTestUtils.setField(systemAdmin, "role", Role.SYSTEM_ADMIN);
        ReflectionTestUtils.setField(systemAdmin, "firstName", "Sys");
        ReflectionTestUtils.setField(systemAdmin, "lastName", "Admin");

        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", BRANCH_ID);
        ReflectionTestUtils.setField(branch, "name", "Test Branch");

        BankingService service = new BankingService();
        ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(service, "name", "Card Collection");

        appointment = new Appointment();
        ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
        appointment.setCustomer(customer);
        appointment.setBranch(branch);
        appointment.setService(service);
        appointment.setAppointmentDate(LocalDate.now().plusDays(7));
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(9, 15));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReferenceNumber("CAP-2026-ABCDE");

        lenient().when(appointmentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- cancel ---

    @Test
    void cancel_pendingByCustomer_succeeds() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.cancelAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, "Changed plans");

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentHistoryRepository).save(any(AppointmentHistory.class));
    }

    @Test
    void cancel_confirmedByCustomer_futureAppointment_succeeds() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.cancelAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, null);

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_confirmedByBranchAdmin_succeeds() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        when(userRepository.findByEmail(BRANCH_ADMIN_EMAIL)).thenReturn(Optional.of(branchAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.cancelAppointment(APPOINTMENT_ID, BRANCH_ADMIN_EMAIL, "Branch closed");

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_otherCustomerAppointment_throws403() {
        User otherCustomer = new User();
        ReflectionTestUtils.setField(otherCustomer, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(otherCustomer, "role", Role.CUSTOMER);
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(otherCustomer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancel_alreadyCancelled_throws422() {
        appointment.setStatus(AppointmentStatus.CANCELLED);
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, null))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void cancel_completedAppointment_throws422() {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        when(userRepository.findByEmail(BRANCH_ADMIN_EMAIL)).thenReturn(Optional.of(branchAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(APPOINTMENT_ID, BRANCH_ADMIN_EMAIL, null))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // --- confirm ---

    @Test
    void confirm_pendingByBranchAdmin_succeeds() {
        when(userRepository.findByEmail(BRANCH_ADMIN_EMAIL)).thenReturn(Optional.of(branchAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.confirmAppointment(APPOINTMENT_ID, BRANCH_ADMIN_EMAIL, null);

        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void confirm_pendingBySystemAdmin_succeeds() {
        when(userRepository.findByEmail(SYSTEM_ADMIN_EMAIL)).thenReturn(Optional.of(systemAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.confirmAppointment(APPOINTMENT_ID, SYSTEM_ADMIN_EMAIL, null);

        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void confirm_byCustomer_throws403() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.confirmAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirm_alreadyConfirmed_throws422() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        when(userRepository.findByEmail(BRANCH_ADMIN_EMAIL)).thenReturn(Optional.of(branchAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.confirmAppointment(APPOINTMENT_ID, BRANCH_ADMIN_EMAIL, null))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // --- complete ---

    @Test
    void complete_confirmedByBranchAdmin_succeeds() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        when(userRepository.findByEmail(BRANCH_ADMIN_EMAIL)).thenReturn(Optional.of(branchAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.completeAppointment(APPOINTMENT_ID, BRANCH_ADMIN_EMAIL, null);

        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void complete_pendingAppointment_throws422() {
        when(userRepository.findByEmail(BRANCH_ADMIN_EMAIL)).thenReturn(Optional.of(branchAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.completeAppointment(APPOINTMENT_ID, BRANCH_ADMIN_EMAIL, null))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CONFIRMED");
    }

    // --- no-show ---

    @Test
    void noShow_confirmedByBranchAdmin_succeeds() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        when(userRepository.findByEmail(BRANCH_ADMIN_EMAIL)).thenReturn(Optional.of(branchAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.markNoShow(APPOINTMENT_ID, BRANCH_ADMIN_EMAIL, null);

        assertThat(response.status()).isEqualTo(AppointmentStatus.NO_SHOW);
    }

    @Test
    void noShow_pendingAppointment_throws422() {
        when(userRepository.findByEmail(BRANCH_ADMIN_EMAIL)).thenReturn(Optional.of(branchAdmin));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.markNoShow(APPOINTMENT_ID, BRANCH_ADMIN_EMAIL, null))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // --- reschedule ---

    @Test
    void reschedule_confirmedByCustomer_createsNewAppointment() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        Branch newBranch = new Branch();
        ReflectionTestUtils.setField(newBranch, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(newBranch, "name", "New Branch");
        ReflectionTestUtils.setField(newBranch, "maxConcurrentAppointments", 5);

        BankingService newService = new BankingService();
        ReflectionTestUtils.setField(newService, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(newService, "name", "Account Assistance");
        ReflectionTestUtils.setField(newService, "durationMinutes", 30);

        BranchOperatingHours hours = new BranchOperatingHours();
        hours.setOpenTime(LocalTime.of(8, 0));
        hours.setCloseTime(LocalTime.of(17, 0));
        hours.setClosed(false);

        LocalDate newDate = LocalDate.now().plusDays(14);
        // ensure it's a weekday
        while (newDate.getDayOfWeek().getValue() > 5) newDate = newDate.plusDays(1);

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(
                newBranch.getId(), newService.getId(), newDate, LocalTime.of(10, 0), "Reschedule reason", "New notes");

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(branchRepository.findByIdAndActiveTrueForUpdate(newBranch.getId())).thenReturn(Optional.of(newBranch));
        when(bankingServiceRepository.findByIdAndActiveTrue(newService.getId())).thenReturn(Optional.of(newService));
        when(publicHolidayRepository.existsByDate(newDate)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(any(), any())).thenReturn(Optional.of(hours));
        when(appointmentRepository.countActiveByCustomerAndDateTime(any(), any(), any(), any())).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsAt(any(), any(), any(), any())).thenReturn(0L);

        AppointmentResponse response = appointmentService.rescheduleAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, request);

        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        verify(appointmentHistoryRepository).save(any(AppointmentHistory.class));
        verify(appointmentRepository, times(2)).save(any(Appointment.class));
    }

    @Test
    void reschedule_pendingAppointment_throws422() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(7),
                LocalTime.of(10, 0), null, null);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, request))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CONFIRMED");
    }

    // --- history ---

    @Test
    void getHistory_ownerCanView() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(appointmentHistoryRepository.findByAppointmentIdOrderByChangedAtAsc(APPOINTMENT_ID))
                .thenReturn(List.of());

        List<AppointmentHistoryResponse> history =
                appointmentService.getAppointmentHistory(APPOINTMENT_ID, CUSTOMER_EMAIL);

        assertThat(history).isEmpty();
    }

    @Test
    void getHistory_nonOwnerCustomer_throws403() {
        User otherCustomer = new User();
        ReflectionTestUtils.setField(otherCustomer, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(otherCustomer, "role", Role.CUSTOMER);
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(otherCustomer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.getAppointmentHistory(APPOINTMENT_ID, CUSTOMER_EMAIL))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getHistory_notFound_throws404() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.getAppointmentHistory(APPOINTMENT_ID, CUSTOMER_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- cancel: past confirmed appointment by customer ----------------------

    @Test
    void cancel_confirmedByCustomer_pastAppointment_throws422() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        // Set appointment to a past date+time so it cannot be cancelled by a customer
        appointment.setAppointmentDate(LocalDate.now().minusDays(1));
        appointment.setStartTime(LocalTime.of(9, 0));

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, null))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("already passed");
    }

    // --- loadUser: unknown email throws 404 -----------------------------------

    @Test
    void loadUser_unknownEmail_throws404() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancelAppointment(APPOINTMENT_ID, "ghost@test.com", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- reschedule: error paths ----------------------------------------------

    @Test
    void reschedule_branchNotFound_throws404() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        UUID unknownBranchId = UUID.randomUUID();

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(branchRepository.findByIdAndActiveTrueForUpdate(unknownBranchId)).thenReturn(Optional.empty());

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(
                unknownBranchId, UUID.randomUUID(), LocalDate.now().plusDays(7),
                LocalTime.of(10, 0), null, null);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Branch not found");
    }

    @Test
    void reschedule_serviceNotFound_throws404() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        UUID unknownServiceId = UUID.randomUUID();

        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(branch, "maxConcurrentAppointments", 5);

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(branchRepository.findByIdAndActiveTrueForUpdate(branch.getId())).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(unknownServiceId)).thenReturn(Optional.empty());

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(
                branch.getId(), unknownServiceId, LocalDate.now().plusDays(7),
                LocalTime.of(10, 0), null, null);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
    }

    @Test
    void reschedule_onPublicHoliday_throwsIllegalArgument() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        LocalDate holiday = LocalDate.now().plusDays(7);

        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(branch, "maxConcurrentAppointments", 5);

        BankingService service = new BankingService();
        ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(service, "durationMinutes", 15);

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(branchRepository.findByIdAndActiveTrueForUpdate(branch.getId())).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(service.getId())).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(holiday)).thenReturn(true);

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(
                branch.getId(), service.getId(), holiday, LocalTime.of(10, 0), null, null);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("public holiday");
    }

    @Test
    void reschedule_branchClosedThatDay_throwsIllegalArgument() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        LocalDate date = LocalDate.now().plusDays(7);

        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(branch, "maxConcurrentAppointments", 5);

        BankingService service = new BankingService();
        ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(service, "durationMinutes", 15);

        BranchOperatingHours closedHours = new BranchOperatingHours();
        closedHours.setClosed(true);

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(branchRepository.findByIdAndActiveTrueForUpdate(branch.getId())).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(service.getId())).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(date)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(any(), any()))
                .thenReturn(Optional.of(closedHours));

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(
                branch.getId(), service.getId(), date, LocalTime.of(10, 0), null, null);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void reschedule_noOperatingHoursForDay_throwsIllegalArgument() {
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        LocalDate date = LocalDate.now().plusDays(7);

        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(branch, "maxConcurrentAppointments", 5);

        BankingService service = new BankingService();
        ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(service, "durationMinutes", 15);

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        when(branchRepository.findByIdAndActiveTrueForUpdate(branch.getId())).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(service.getId())).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(date)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(any(), any()))
                .thenReturn(Optional.empty());

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(
                branch.getId(), service.getId(), date, LocalTime.of(10, 0), null, null);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(APPOINTMENT_ID, CUSTOMER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closed");
    }
}
