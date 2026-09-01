package com.capitec.capibook.appointment;

import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.appointment.dto.CreateAppointmentRequest;
import com.capitec.capibook.availability.PublicHolidayRepository;
import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchOperatingHours;
import com.capitec.capibook.branch.BranchOperatingHoursRepository;
import com.capitec.capibook.branch.BranchRepository;
import com.capitec.capibook.exception.AppointmentConflictException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.servicecatalog.BankingService;
import com.capitec.capibook.servicecatalog.BankingServiceRepository;
import com.capitec.capibook.user.Role;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentHistoryRepository appointmentHistoryRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private BankingServiceRepository bankingServiceRepository;
    @Mock private BranchOperatingHoursRepository operatingHoursRepository;
    @Mock private PublicHolidayRepository publicHolidayRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    private static final String CUSTOMER_EMAIL = "customer@test.com";
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(7);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);

    private User customer;
    private Branch branch;
    private BankingService service;
    private CreateAppointmentRequest validRequest;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setEmail(CUSTOMER_EMAIL);
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setRole(Role.CUSTOMER);

        branch = new Branch();
        branch.setBranchCode("CPT001");
        branch.setName("Cape Town Main");
        branch.setAddress("1 Adderley Street");
        branch.setCity("Cape Town");
        branch.setProvince("Western Cape");
        branch.setPostalCode("8001");
        branch.setMaxConcurrentAppointments(1);

        service = new BankingService();
        service.setName("Card Collection");
        service.setDurationMinutes(30);

        validRequest = new CreateAppointmentRequest(
                UUID.randomUUID(), UUID.randomUUID(), FUTURE_DATE, START_TIME, null);
    }

    @Test
    void createAppointment_branchNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(branchRepository.findByIdAndActiveTrueForUpdate(validRequest.branchId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(validRequest.branchId().toString());
    }

    @Test
    void createAppointment_serviceNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(branchRepository.findByIdAndActiveTrueForUpdate(validRequest.branchId()))
                .thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(validRequest.serviceId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(validRequest.serviceId().toString());
    }

    @Test
    void createAppointment_onPublicHoliday_throwsIllegalArgument() {
        stubBranchAndService();
        when(publicHolidayRepository.existsByDate(FUTURE_DATE)).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("public holiday");
    }

    @Test
    void createAppointment_branchHasNoHoursForDay_throwsIllegalArgument() {
        stubBranchAndService();
        when(publicHolidayRepository.existsByDate(FUTURE_DATE)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branch.getId(), FUTURE_DATE.getDayOfWeek()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void createAppointment_branchClosedThatDay_throwsIllegalArgument() {
        stubBranchAndService();
        when(publicHolidayRepository.existsByDate(FUTURE_DATE)).thenReturn(false);
        BranchOperatingHours closedHours = makeHours(FUTURE_DATE.getDayOfWeek(), "08:00", "17:00", true);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branch.getId(), FUTURE_DATE.getDayOfWeek()))
                .thenReturn(Optional.of(closedHours));

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void createAppointment_slotBeforeOpenTime_throwsIllegalArgument() {
        stubBranchAndService();
        when(publicHolidayRepository.existsByDate(FUTURE_DATE)).thenReturn(false);
        // Branch opens at 10:00, but slot is 09:00
        BranchOperatingHours hours = makeHours(FUTURE_DATE.getDayOfWeek(), "10:00", "17:00", false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branch.getId(), FUTURE_DATE.getDayOfWeek()))
                .thenReturn(Optional.of(hours));

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside branch operating hours");
    }

    @Test
    void createAppointment_slotExceedsClosingTime_throwsIllegalArgument() {
        service.setDurationMinutes(60);
        stubBranchAndService();
        when(publicHolidayRepository.existsByDate(FUTURE_DATE)).thenReturn(false);
        // Slot 09:00 + 60 min = 10:00, branch closes at 09:30
        BranchOperatingHours hours = makeHours(FUTURE_DATE.getDayOfWeek(), "08:00", "09:30", false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branch.getId(), FUTURE_DATE.getDayOfWeek()))
                .thenReturn(Optional.of(hours));

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beyond branch closing time");
    }

    @Test
    void createAppointment_customerAlreadyBookedSameSlot_throwsConflict() {
        stubBranchAndService();
        stubOpenHours();
        when(publicHolidayRepository.existsByDate(FUTURE_DATE)).thenReturn(false);
        when(appointmentRepository.countActiveByCustomerAndDateTime(
                eq(customer.getId()), eq(FUTURE_DATE), eq(START_TIME), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessageContaining("already have an appointment");
    }

    @Test
    void createAppointment_slotAtCapacity_throwsConflict() {
        stubBranchAndService();
        stubOpenHours();
        when(publicHolidayRepository.existsByDate(FUTURE_DATE)).thenReturn(false);
        when(appointmentRepository.countActiveByCustomerAndDateTime(any(), any(), any(), any())).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsAt(
                eq(branch.getId()), eq(FUTURE_DATE), eq(START_TIME), any()))
                .thenReturn(1L); // capacity is 1

        assertThatThrownBy(() -> appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void createAppointment_success_returnsResponse() {
        stubBranchAndService();
        stubOpenHours();
        when(publicHolidayRepository.existsByDate(FUTURE_DATE)).thenReturn(false);
        when(appointmentRepository.countActiveByCustomerAndDateTime(any(), any(), any(), any())).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsAt(any(), any(), any(), any())).thenReturn(0L);

        Appointment saved = new Appointment();
        saved.setCustomer(customer);
        saved.setBranch(branch);
        saved.setService(service);
        saved.setAppointmentDate(FUTURE_DATE);
        saved.setStartTime(START_TIME);
        saved.setEndTime(START_TIME.plusMinutes(service.getDurationMinutes()));
        saved.setStatus(AppointmentStatus.PENDING);
        saved.setReferenceNumber("CAP-2026-ABCD1");
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentResponse response = appointmentService.createAppointment(CUSTOMER_EMAIL, validRequest);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(response.customerFirstName()).isEqualTo("Jane");
        assertThat(response.branchName()).isEqualTo("Cape Town Main");
        assertThat(response.serviceName()).isEqualTo("Card Collection");
        assertThat(response.startTime()).isEqualTo(START_TIME);
        assertThat(response.endTime()).isEqualTo(START_TIME.plusMinutes(30));

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceNumber()).startsWith("CAP-");
    }

    @Test
    void getAppointmentById_asCustomerOwner_returnsResponse() {
        User caller = new User();
        caller.setRole(Role.CUSTOMER);
        UUID callerId = UUID.randomUUID();
        ReflectionTestUtils.setField(caller, "id", callerId);

        Appointment appointment = new Appointment();
        appointment.setCustomer(caller);
        appointment.setBranch(branch);
        appointment.setService(service);
        appointment.setAppointmentDate(FUTURE_DATE);
        appointment.setStartTime(START_TIME);
        appointment.setEndTime(START_TIME.plusMinutes(30));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReferenceNumber("CAP-2026-ABCD1");

        UUID appointmentId = UUID.randomUUID();
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(caller));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.getAppointmentById(appointmentId, CUSTOMER_EMAIL);

        assertThat(response).isNotNull();
    }

    @Test
    void getAppointmentById_asCustomerNonOwner_throwsAccessDenied() {
        User caller = new User();
        caller.setRole(Role.CUSTOMER);
        ReflectionTestUtils.setField(caller, "id", UUID.randomUUID());

        User otherCustomer = new User();
        ReflectionTestUtils.setField(otherCustomer, "id", UUID.randomUUID());
        otherCustomer.setFirstName("Other");
        otherCustomer.setLastName("User");

        Appointment appointment = new Appointment();
        appointment.setCustomer(otherCustomer);
        appointment.setBranch(branch);
        appointment.setService(service);
        appointment.setAppointmentDate(FUTURE_DATE);
        appointment.setStartTime(START_TIME);
        appointment.setEndTime(START_TIME.plusMinutes(30));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReferenceNumber("CAP-2026-ABCD1");

        UUID appointmentId = UUID.randomUUID();
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(caller));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.getAppointmentById(appointmentId, CUSTOMER_EMAIL))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAppointmentById_asSystemAdmin_returnsAnyAppointment() {
        User admin = new User();
        admin.setRole(Role.SYSTEM_ADMIN);

        User otherCustomer = new User();
        ReflectionTestUtils.setField(otherCustomer, "id", UUID.randomUUID());
        otherCustomer.setFirstName("Other");
        otherCustomer.setLastName("Customer");

        Appointment appointment = new Appointment();
        appointment.setCustomer(otherCustomer);
        appointment.setBranch(branch);
        appointment.setService(service);
        appointment.setAppointmentDate(FUTURE_DATE);
        appointment.setStartTime(START_TIME);
        appointment.setEndTime(START_TIME.plusMinutes(30));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReferenceNumber("CAP-2026-ABCD1");

        UUID appointmentId = UUID.randomUUID();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService.getAppointmentById(appointmentId, "admin@test.com");

        assertThat(response).isNotNull();
    }

    @Test
    void getAppointmentById_notFound_throwsResourceNotFoundException() {
        User caller = new User();
        caller.setRole(Role.CUSTOMER);
        UUID appointmentId = UUID.randomUUID();

        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(caller));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.getAppointmentById(appointmentId, CUSTOMER_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- helpers ---

    private void stubBranchAndService() {
        when(userRepository.findByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));
        when(branchRepository.findByIdAndActiveTrueForUpdate(validRequest.branchId()))
                .thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(validRequest.serviceId()))
                .thenReturn(Optional.of(service));
    }

    private void stubOpenHours() {
        BranchOperatingHours hours = makeHours(FUTURE_DATE.getDayOfWeek(), "08:00", "17:00", false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branch.getId(), FUTURE_DATE.getDayOfWeek()))
                .thenReturn(Optional.of(hours));
    }

    private BranchOperatingHours makeHours(DayOfWeek day, String open, String close, boolean closed) {
        BranchOperatingHours h = new BranchOperatingHours();
        h.setDayOfWeek(day);
        h.setOpenTime(LocalTime.parse(open));
        h.setCloseTime(LocalTime.parse(close));
        h.setClosed(closed);
        return h;
    }
}
