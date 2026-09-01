package com.capitec.capibook.availability;

import com.capitec.capibook.appointment.AppointmentRepository;
import com.capitec.capibook.appointment.AppointmentStatus;
import com.capitec.capibook.availability.dto.AvailabilityResponse;
import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchAvailabilityExceptionRepository;
import com.capitec.capibook.branch.BranchOperatingHours;
import com.capitec.capibook.branch.BranchOperatingHoursRepository;
import com.capitec.capibook.branch.BranchRepository;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.servicecatalog.BankingService;
import com.capitec.capibook.servicecatalog.BankingServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock private BranchRepository branchRepository;
    @Mock private BankingServiceRepository bankingServiceRepository;
    @Mock private BranchOperatingHoursRepository operatingHoursRepository;
    @Mock private PublicHolidayRepository publicHolidayRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private BranchAvailabilityExceptionRepository availabilityExceptionRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private UUID branchId;
    private UUID serviceId;
    private Branch branch;
    private BankingService service;

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();
        serviceId = UUID.randomUUID();

        branch = new Branch();
        branch.setBranchCode("CPT001");
        branch.setName("Cape Town Main");
        branch.setAddress("1 Adderley Street");
        branch.setCity("Cape Town");
        branch.setProvince("Western Cape");
        branch.setPostalCode("8001");
        branch.setActive(true);
        branch.setMaxConcurrentAppointments(1);

        service = new BankingService();
        service.setName("Card Collection");
        service.setDurationMinutes(30);
    }

    @Test
    void getAvailability_withInactiveBranch_throwsResourceNotFoundException() {
        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getAvailability(branchId, serviceId, LocalDate.now().plusDays(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(branchId.toString());
    }

    @Test
    void getAvailability_withInactiveService_throwsResourceNotFoundException() {
        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getAvailability(branchId, serviceId, LocalDate.now().plusDays(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(serviceId.toString());
    }

    @Test
    void getAvailability_onPublicHoliday_returnsEmptySlots() {
        LocalDate holiday = LocalDate.of(2025, 12, 25);
        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(holiday)).thenReturn(true);

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, holiday);

        assertThat(response.slots()).isEmpty();
    }

    @Test
    void getAvailability_whenBranchHasNoHoursForDay_returnsEmptySlots() {
        LocalDate sunday = LocalDate.of(2025, 6, 15); // Sunday
        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(sunday)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, DayOfWeek.SUNDAY))
                .thenReturn(Optional.empty());

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, sunday);

        assertThat(response.slots()).isEmpty();
    }

    @Test
    void getAvailability_whenBranchIsClosedThatDay_returnsEmptySlots() {
        LocalDate saturday = LocalDate.of(2025, 6, 14); // Saturday
        BranchOperatingHours closedHours = new BranchOperatingHours();
        closedHours.setDayOfWeek(DayOfWeek.SATURDAY);
        closedHours.setClosed(true);

        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(saturday)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, DayOfWeek.SATURDAY))
                .thenReturn(Optional.of(closedHours));

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, saturday);

        assertThat(response.slots()).isEmpty();
    }

    @Test
    void getAvailability_withNormalDay_generatesCorrectSlots() {
        LocalDate monday = LocalDate.of(2025, 6, 16); // Youth Day — but we mock holiday check as false
        BranchOperatingHours hours = openHours(DayOfWeek.MONDAY, "08:00", "10:00");

        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(monday)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(hours));
        when(appointmentRepository.countBookedSlotsAt(eq(branchId), eq(monday), any(), any())).thenReturn(0L);

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, monday);

        // 08:00–10:00 with 30-min slots → 08:00, 08:30, 09:00, 09:30 (4 slots)
        assertThat(response.slots()).hasSize(4);
        assertThat(response.slots().get(0).startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.slots().get(0).endTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(response.slots().get(3).startTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(response.slots().get(3).endTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.slots()).allMatch(s -> s.status() == SlotStatus.AVAILABLE);
    }

    @Test
    void getAvailability_lastSlotDoesNotExceedClosingTime() {
        // 08:00–09:00 with 30-min slots → only 08:00 and 08:30 should appear; 09:00 would end at 09:30 which exceeds closing
        LocalDate monday = LocalDate.of(2025, 6, 9);
        service.setDurationMinutes(40); // 40-min service
        BranchOperatingHours hours = openHours(DayOfWeek.MONDAY, "08:00", "09:00");

        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(monday)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(hours));
        when(appointmentRepository.countBookedSlotsAt(eq(branchId), eq(monday), any(), any())).thenReturn(0L);

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, monday);

        // 08:00+40min = 08:40 ≤ 09:00 → slot generated
        // 08:40+40min = 09:20 > 09:00 → no second slot
        assertThat(response.slots()).hasSize(1);
        assertThat(response.slots().get(0).startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.slots().get(0).endTime()).isEqualTo(LocalTime.of(8, 40));
    }

    @Test
    void getAvailability_bookedSlotShowsBookedStatus() {
        LocalDate monday = LocalDate.of(2025, 6, 9);
        BranchOperatingHours hours = openHours(DayOfWeek.MONDAY, "08:00", "09:00");

        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(monday)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(hours));
        // 08:00 slot is fully booked (capacity = 1, count = 1)
        when(appointmentRepository.countBookedSlotsAt(
                eq(branchId), eq(monday), eq(LocalTime.of(8, 0)),
                eq(AvailabilityService.ACTIVE_STATUSES))).thenReturn(1L);
        when(appointmentRepository.countBookedSlotsAt(
                eq(branchId), eq(monday), eq(LocalTime.of(8, 30)),
                eq(AvailabilityService.ACTIVE_STATUSES))).thenReturn(0L);

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, monday);

        assertThat(response.slots()).hasSize(2);
        assertThat(response.slots().get(0).status()).isEqualTo(SlotStatus.BOOKED);
        assertThat(response.slots().get(1).status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void getAvailability_withCapacityGreaterThanOne_slotAvailableUntilCapacityExceeded() {
        branch.setMaxConcurrentAppointments(2);
        LocalDate monday = LocalDate.of(2025, 6, 9);
        BranchOperatingHours hours = openHours(DayOfWeek.MONDAY, "08:00", "08:30");

        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(monday)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(hours));
        // 1 booking against capacity 2 → still AVAILABLE
        when(appointmentRepository.countBookedSlotsAt(any(), any(), any(), any())).thenReturn(1L);

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, monday);

        assertThat(response.slots()).hasSize(1);
        assertThat(response.slots().get(0).status()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void getAvailability_responseContainsCorrectBranchAndServiceIds() {
        LocalDate monday = LocalDate.of(2025, 6, 9);
        BranchOperatingHours hours = openHours(DayOfWeek.MONDAY, "08:00", "08:30");

        when(branchRepository.findByIdAndActiveTrue(branchId)).thenReturn(Optional.of(branch));
        when(bankingServiceRepository.findByIdAndActiveTrue(serviceId)).thenReturn(Optional.of(service));
        when(publicHolidayRepository.existsByDate(monday)).thenReturn(false);
        when(operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(hours));
        when(appointmentRepository.countBookedSlotsAt(any(), any(), any(), any())).thenReturn(0L);

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, monday);

        assertThat(response.branchId()).isEqualTo(branchId);
        assertThat(response.serviceId()).isEqualTo(serviceId);
        assertThat(response.date()).isEqualTo(monday);
    }

    private BranchOperatingHours openHours(DayOfWeek day, String open, String close) {
        BranchOperatingHours h = new BranchOperatingHours();
        h.setDayOfWeek(day);
        h.setOpenTime(LocalTime.parse(open));
        h.setCloseTime(LocalTime.parse(close));
        h.setClosed(false);
        return h;
    }
}
