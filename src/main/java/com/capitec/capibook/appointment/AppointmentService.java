package com.capitec.capibook.appointment;

import com.capitec.capibook.appointment.dto.AppointmentHistoryResponse;
import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.appointment.dto.CreateAppointmentRequest;
import com.capitec.capibook.appointment.dto.RescheduleAppointmentRequest;
import com.capitec.capibook.availability.PublicHolidayRepository;
import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchOperatingHours;
import com.capitec.capibook.branch.BranchOperatingHoursRepository;
import com.capitec.capibook.branch.BranchRepository;
import com.capitec.capibook.common.PageResponse;
import com.capitec.capibook.exception.AppointmentConflictException;
import com.capitec.capibook.exception.InvalidStatusTransitionException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.kafka.AppointmentDomainEvent;
import com.capitec.capibook.kafka.events.AppointmentEventMessage;
import com.capitec.capibook.kafka.events.AppointmentEventType;
import com.capitec.capibook.servicecatalog.BankingService;
import com.capitec.capibook.servicecatalog.BankingServiceRepository;
import com.capitec.capibook.user.Role;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AppointmentService {

    static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private static final List<AppointmentStatus> TERMINAL_STATUSES =
            List.of(AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED,
                    AppointmentStatus.NO_SHOW, AppointmentStatus.RESCHEDULED);

    private static final Map<AppointmentStatus, AppointmentEventType> STATUS_TO_EVENT = Map.of(
            AppointmentStatus.CANCELLED,    AppointmentEventType.APPOINTMENT_CANCELLED,
            AppointmentStatus.CONFIRMED,    AppointmentEventType.APPOINTMENT_CONFIRMED,
            AppointmentStatus.COMPLETED,    AppointmentEventType.APPOINTMENT_COMPLETED,
            AppointmentStatus.NO_SHOW,      AppointmentEventType.APPOINTMENT_NO_SHOW
    );

    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final BranchRepository branchRepository;
    private final BankingServiceRepository bankingServiceRepository;
    private final BranchOperatingHoursRepository operatingHoursRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              AppointmentHistoryRepository appointmentHistoryRepository,
                              BranchRepository branchRepository,
                              BankingServiceRepository bankingServiceRepository,
                              BranchOperatingHoursRepository operatingHoursRepository,
                              PublicHolidayRepository publicHolidayRepository,
                              UserRepository userRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentHistoryRepository = appointmentHistoryRepository;
        this.branchRepository = branchRepository;
        this.bankingServiceRepository = bankingServiceRepository;
        this.operatingHoursRepository = operatingHoursRepository;
        this.publicHolidayRepository = publicHolidayRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
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
        User customer = loadUser(customerEmail);

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

        Appointment saved = appointmentRepository.save(appointment);
        publishEvent(saved, AppointmentEventType.APPOINTMENT_CREATED);
        return toResponse(saved, customer, branch, service);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID appointmentId, String callerEmail) {
        User caller = loadUser(callerEmail);
        Appointment appointment = loadAppointment(appointmentId);
        enforceOwnerOrAdmin(appointment, caller);
        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> getMyAppointments(String customerEmail, Pageable pageable) {
        User customer = loadUser(customerEmail);

        Page<Appointment> page = appointmentRepository.findByCustomerId(customer.getId(), pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public AppointmentResponse cancelAppointment(UUID appointmentId, String callerEmail, String reason) {
        User caller = loadUser(callerEmail);
        Appointment appointment = loadAppointment(appointmentId);
        enforceOwnerOrAdmin(appointment, caller);

        AppointmentStatus current = appointment.getStatus();
        if (TERMINAL_STATUSES.contains(current)) {
            throw new InvalidStatusTransitionException(
                    "Cannot cancel an appointment with status: " + current);
        }
        if (current != AppointmentStatus.PENDING && current != AppointmentStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException(
                    "Cannot cancel an appointment with status: " + current);
        }
        if (caller.getRole() == Role.CUSTOMER && current == AppointmentStatus.CONFIRMED) {
            LocalDateTime appointmentDateTime = LocalDateTime.of(
                    appointment.getAppointmentDate(), appointment.getStartTime());
            if (!appointmentDateTime.isAfter(LocalDateTime.now())) {
                throw new InvalidStatusTransitionException(
                        "Cannot cancel a confirmed appointment that has already passed");
            }
        }

        return transitionStatus(appointment, caller, AppointmentStatus.CANCELLED, reason);
    }

    @Transactional
    public AppointmentResponse confirmAppointment(UUID appointmentId, String callerEmail, String reason) {
        User caller = loadUser(callerEmail);
        Appointment appointment = loadAppointment(appointmentId);
        requireAdminRole(caller);
        enforceBranchAdminBranchRestriction(appointment, caller);

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    "Only PENDING appointments can be confirmed. Current status: " + appointment.getStatus());
        }

        return transitionStatus(appointment, caller, AppointmentStatus.CONFIRMED, reason);
    }

    @Transactional
    public AppointmentResponse completeAppointment(UUID appointmentId, String callerEmail, String reason) {
        User caller = loadUser(callerEmail);
        Appointment appointment = loadAppointment(appointmentId);
        requireAdminRole(caller);
        enforceBranchAdminBranchRestriction(appointment, caller);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException(
                    "Only CONFIRMED appointments can be completed. Current status: " + appointment.getStatus());
        }

        return transitionStatus(appointment, caller, AppointmentStatus.COMPLETED, reason);
    }

    @Transactional
    public AppointmentResponse markNoShow(UUID appointmentId, String callerEmail, String reason) {
        User caller = loadUser(callerEmail);
        Appointment appointment = loadAppointment(appointmentId);
        requireAdminRole(caller);
        enforceBranchAdminBranchRestriction(appointment, caller);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException(
                    "Only CONFIRMED appointments can be marked as no-show. Current status: " + appointment.getStatus());
        }

        return transitionStatus(appointment, caller, AppointmentStatus.NO_SHOW, reason);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(UUID appointmentId, String callerEmail,
                                                     RescheduleAppointmentRequest request) {
        User caller = loadUser(callerEmail);
        Appointment original = loadAppointment(appointmentId);
        enforceOwnerOrAdmin(original, caller);

        if (original.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException(
                    "Only CONFIRMED appointments can be rescheduled. Current status: " + original.getStatus());
        }

        Branch branch = branchRepository.findByIdAndActiveTrueForUpdate(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.branchId()));

        BankingService service = bankingServiceRepository.findByIdAndActiveTrue(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + request.serviceId()));

        LocalDate date = request.appointmentDate();
        LocalTime startTime = request.startTime();
        LocalTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        if (publicHolidayRepository.existsByDate(date)) {
            throw new IllegalArgumentException("Cannot reschedule to a public holiday");
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
                caller.getRole() == Role.CUSTOMER ? caller.getId() : original.getCustomer().getId(),
                date, startTime, ACTIVE_STATUSES);
        if (customerConflict > 0) {
            throw new AppointmentConflictException("Customer already has an appointment at this date and time");
        }

        long booked = appointmentRepository.countBookedSlotsAt(
                branch.getId(), date, startTime, ACTIVE_STATUSES);
        if (booked >= branch.getMaxConcurrentAppointments()) {
            throw new AppointmentConflictException("This time slot is no longer available");
        }

        recordHistory(original, caller, AppointmentStatus.RESCHEDULED, request.reason());
        original.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.save(original);

        Appointment rescheduled = new Appointment();
        rescheduled.setCustomer(original.getCustomer());
        rescheduled.setBranch(branch);
        rescheduled.setService(service);
        rescheduled.setAppointmentDate(date);
        rescheduled.setStartTime(startTime);
        rescheduled.setEndTime(endTime);
        rescheduled.setStatus(AppointmentStatus.PENDING);
        rescheduled.setReferenceNumber(generateReferenceNumber(date.getYear()));
        rescheduled.setNotes(request.notes());

        Appointment savedRescheduled = appointmentRepository.save(rescheduled);
        publishEvent(savedRescheduled, AppointmentEventType.APPOINTMENT_RESCHEDULED);
        return toResponse(savedRescheduled);
    }

    @Transactional(readOnly = true)
    public List<AppointmentHistoryResponse> getAppointmentHistory(UUID appointmentId, String callerEmail) {
        User caller = loadUser(callerEmail);
        Appointment appointment = loadAppointment(appointmentId);

        if (caller.getRole() == Role.CUSTOMER && !appointment.getCustomer().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        return appointmentHistoryRepository.findByAppointmentIdOrderByChangedAtAsc(appointmentId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private AppointmentResponse transitionStatus(Appointment appointment, User changedBy,
                                                  AppointmentStatus newStatus, String reason) {
        recordHistory(appointment, changedBy, newStatus, reason);
        appointment.setStatus(newStatus);
        Appointment saved = appointmentRepository.save(appointment);
        AppointmentEventType eventType = STATUS_TO_EVENT.get(newStatus);
        if (eventType != null) {
            publishEvent(saved, eventType);
        }
        return toResponse(saved);
    }

    private void recordHistory(Appointment appointment, User changedBy,
                                AppointmentStatus newStatus, String reason) {
        AppointmentHistory history = new AppointmentHistory();
        history.setAppointment(appointment);
        history.setPreviousStatus(appointment.getStatus());
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setChangeReason(reason);
        appointmentHistoryRepository.save(history);
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Appointment loadAppointment(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    private void enforceOwnerOrAdmin(Appointment appointment, User caller) {
        if (caller.getRole() == Role.CUSTOMER) {
            if (!appointment.getCustomer().getId().equals(caller.getId())) {
                throw new AccessDeniedException("Access denied");
            }
        } else if (caller.getRole() == Role.BRANCH_ADMIN) {
            if (caller.getBranchId() == null
                    || !appointment.getBranch().getId().equals(caller.getBranchId())) {
                throw new AccessDeniedException("Access denied");
            }
        }
        // SYSTEM_ADMIN: no restriction
    }

    private void requireAdminRole(User caller) {
        if (caller.getRole() != Role.BRANCH_ADMIN && caller.getRole() != Role.SYSTEM_ADMIN) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void enforceBranchAdminBranchRestriction(Appointment appointment, User caller) {
        if (caller.getRole() == Role.BRANCH_ADMIN) {
            if (caller.getBranchId() == null
                    || !appointment.getBranch().getId().equals(caller.getBranchId())) {
                throw new AccessDeniedException("Access denied");
            }
        }
    }

    private AppointmentHistoryResponse toHistoryResponse(AppointmentHistory h) {
        return new AppointmentHistoryResponse(
                h.getId(),
                h.getAppointment().getId(),
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getChangedBy().getId(),
                h.getChangedBy().getFirstName(),
                h.getChangedBy().getLastName(),
                h.getChangeReason(),
                h.getChangedAt()
        );
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

    private void publishEvent(Appointment appointment, AppointmentEventType eventType) {
        AppointmentEventMessage message = new AppointmentEventMessage(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                appointment.getId(),
                appointment.getCustomer().getId(),
                appointment.getBranch().getId(),
                appointment.getService().getId(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getReferenceNumber()
        );
        eventPublisher.publishEvent(new AppointmentDomainEvent(this, message));
    }
}
