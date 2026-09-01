package com.capitec.capibook.appointment;

import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.appointment.dto.CreateAppointmentRequest;
import com.capitec.capibook.availability.PublicHolidayRepository;
import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchOperatingHours;
import com.capitec.capibook.branch.BranchOperatingHoursRepository;
import com.capitec.capibook.branch.BranchRepository;
import com.capitec.capibook.common.PageResponse;
import com.capitec.capibook.exception.AppointmentConflictException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.servicecatalog.BankingService;
import com.capitec.capibook.servicecatalog.BankingServiceRepository;
import com.capitec.capibook.user.Role;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final BranchRepository branchRepository;
    private final BankingServiceRepository bankingServiceRepository;
    private final BranchOperatingHoursRepository operatingHoursRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              BranchRepository branchRepository,
                              BankingServiceRepository bankingServiceRepository,
                              BranchOperatingHoursRepository operatingHoursRepository,
                              PublicHolidayRepository publicHolidayRepository,
                              UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.branchRepository = branchRepository;
        this.bankingServiceRepository = bankingServiceRepository;
        this.operatingHoursRepository = operatingHoursRepository;
        this.publicHolidayRepository = publicHolidayRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new appointment.
     *
     * Concurrency protection: a pessimistic write lock is acquired on the Branch row
     * before the slot count check. This ensures that two concurrent requests for the
     * same branch slot cannot both pass the count check — one will wait for the other
     * to commit, then re-read the updated count and be rejected if the slot is full.
     */
    @Transactional
    public AppointmentResponse createAppointment(String customerEmail, CreateAppointmentRequest request) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Branch branch = branchRepository.findByIdAndActiveTrueForUpdate(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.branchId()));

        BankingService service = bankingServiceRepository.findByIdAndActiveTrue(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + request.serviceId()));

        LocalDate date = request.appointmentDate();
        LocalTime startTime = request.startTime();
        LocalTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        if (publicHolidayRepository.existsByDate(date)) {
            throw new IllegalArgumentException("Cannot book on a public holiday");
        }

        BranchOperatingHours hours = operatingHoursRepository
                .findByBranchIdAndDayOfWeek(branch.getId(), date.getDayOfWeek())
                .orElseThrow(() -> new IllegalArgumentException("Branch is closed on the requested day"));

        if (hours.isClosed()) {
            throw new IllegalArgumentException("Branch is closed on the requested day");
        }

        if (startTime.isBefore(hours.getOpenTime())) {
            throw new IllegalArgumentException("Requested time slot is outside branch operating hours");
        }

        if (endTime.isAfter(hours.getCloseTime())) {
            throw new IllegalArgumentException("Requested time slot extends beyond branch closing time");
        }

        long customerConflict = appointmentRepository.countActiveByCustomerAndDateTime(
                customer.getId(), date, startTime, ACTIVE_STATUSES);
        if (customerConflict > 0) {
            throw new AppointmentConflictException("You already have an appointment at this date and time");
        }

        long booked = appointmentRepository.countBookedSlotsAt(
                branch.getId(), date, startTime, ACTIVE_STATUSES);
        if (booked >= branch.getMaxConcurrentAppointments()) {
            throw new AppointmentConflictException("This time slot is no longer available");
        }

        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setBranch(branch);
        appointment.setService(service);
        appointment.setAppointmentDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setReferenceNumber(generateReferenceNumber(date.getYear()));
        appointment.setNotes(request.notes());

        return toResponse(appointmentRepository.save(appointment), customer, branch, service);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID appointmentId, String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (caller.getRole() == Role.CUSTOMER && !appointment.getCustomer().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> getMyAppointments(String customerEmail, Pageable pageable) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Appointment> page = appointmentRepository.findByCustomerId(customer.getId(), pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getReferenceNumber(),
                a.getCustomer().getId(),
                a.getCustomer().getFirstName(),
                a.getCustomer().getLastName(),
                a.getBranch().getId(),
                a.getBranch().getName(),
                a.getService().getId(),
                a.getService().getName(),
                a.getAppointmentDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private AppointmentResponse toResponse(Appointment a, User customer, Branch branch, BankingService service) {
        return new AppointmentResponse(
                a.getId(),
                a.getReferenceNumber(),
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                branch.getId(),
                branch.getName(),
                service.getId(),
                service.getName(),
                a.getAppointmentDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private String generateReferenceNumber(int year) {
        return String.format("CAP-%d-%s",
                year,
                UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase());
    }
}
