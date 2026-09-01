package com.capitec.capibook.availability;

import com.capitec.capibook.appointment.AppointmentRepository;
import com.capitec.capibook.appointment.AppointmentStatus;
import com.capitec.capibook.availability.dto.AvailabilityResponse;
import com.capitec.capibook.availability.dto.SlotResponse;
import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchOperatingHours;
import com.capitec.capibook.branch.BranchOperatingHoursRepository;
import com.capitec.capibook.branch.BranchRepository;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.servicecatalog.BankingService;
import com.capitec.capibook.servicecatalog.BankingServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final BranchRepository branchRepository;
    private final BankingServiceRepository bankingServiceRepository;
    private final BranchOperatingHoursRepository operatingHoursRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final AppointmentRepository appointmentRepository;

    public AvailabilityService(BranchRepository branchRepository,
                               BankingServiceRepository bankingServiceRepository,
                               BranchOperatingHoursRepository operatingHoursRepository,
                               PublicHolidayRepository publicHolidayRepository,
                               AppointmentRepository appointmentRepository) {
        this.branchRepository = branchRepository;
        this.bankingServiceRepository = bankingServiceRepository;
        this.operatingHoursRepository = operatingHoursRepository;
        this.publicHolidayRepository = publicHolidayRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public AvailabilityResponse getAvailability(UUID branchId, UUID serviceId, LocalDate date) {
        Branch branch = branchRepository.findByIdAndActiveTrue(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        BankingService service = bankingServiceRepository.findByIdAndActiveTrue(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));

        if (publicHolidayRepository.existsByDate(date)) {
            return new AvailabilityResponse(branchId, serviceId, date, List.of());
        }

        Optional<BranchOperatingHours> hoursOpt =
                operatingHoursRepository.findByBranchIdAndDayOfWeek(branchId, date.getDayOfWeek());

        if (hoursOpt.isEmpty() || hoursOpt.get().isClosed()) {
            return new AvailabilityResponse(branchId, serviceId, date, List.of());
        }

        BranchOperatingHours hours = hoursOpt.get();
        int durationMinutes = service.getDurationMinutes();
        int capacity = branch.getMaxConcurrentAppointments();

        List<SlotResponse> slots = new ArrayList<>();
        LocalTime cursor = hours.getOpenTime();
        LocalTime closing = hours.getCloseTime();

        while (!cursor.plusMinutes(durationMinutes).isAfter(closing)) {
            LocalTime slotEnd = cursor.plusMinutes(durationMinutes);
            long booked = appointmentRepository.countBookedSlotsAt(branchId, date, cursor, ACTIVE_STATUSES);
            SlotStatus status = booked >= capacity ? SlotStatus.BOOKED : SlotStatus.AVAILABLE;
            slots.add(new SlotResponse(cursor, slotEnd, status));
            cursor = cursor.plusMinutes(durationMinutes);
        }

        return new AvailabilityResponse(branchId, serviceId, date, slots);
    }
}
