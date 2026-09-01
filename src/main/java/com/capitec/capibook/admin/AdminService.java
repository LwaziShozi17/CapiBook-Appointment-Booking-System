package com.capitec.capibook.admin;

import com.capitec.capibook.admin.dto.*;
import com.capitec.capibook.appointment.AppointmentRepository;
import com.capitec.capibook.appointment.AppointmentStatus;
import com.capitec.capibook.appointment.dto.AppointmentResponse;
import com.capitec.capibook.audit.AuditLog;
import com.capitec.capibook.audit.AuditLogRepository;
import com.capitec.capibook.branch.Branch;
import com.capitec.capibook.branch.BranchAvailabilityException;
import com.capitec.capibook.branch.BranchAvailabilityExceptionRepository;
import com.capitec.capibook.branch.BranchRepository;
import com.capitec.capibook.common.PageResponse;
import com.capitec.capibook.exception.DuplicateEmailException;
import com.capitec.capibook.exception.DuplicateResourceException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import com.capitec.capibook.user.Role;
import com.capitec.capibook.user.User;
import com.capitec.capibook.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final BranchAvailabilityExceptionRepository availabilityExceptionRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        BranchRepository branchRepository,
                        AppointmentRepository appointmentRepository,
                        AuditLogRepository auditLogRepository,
                        BranchAvailabilityExceptionRepository availabilityExceptionRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.availabilityExceptionRepository = availabilityExceptionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return PageResponse.from(page.map(AdminUserResponse::from));
    }

    @Transactional
    public AdminUserResponse createBranchAdmin(CreateBranchAdminRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException("Email already registered: " + request.email());
        }

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.branchId()));

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(Role.BRANCH_ADMIN);
        user.setBranchId(branch.getId());

        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setActive(false);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> listAppointmentsForAdmin(String callerEmail,
                                                                       UUID branchIdFilter,
                                                                       Pageable pageable) {
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<com.capitec.capibook.appointment.Appointment> page;

        if (caller.getRole() == Role.BRANCH_ADMIN) {
            UUID effectiveBranchId = caller.getBranchId();
            if (effectiveBranchId == null) {
                return PageResponse.from(Page.empty(pageable).map(a -> null));
            }
            page = appointmentRepository.findByBranchId(effectiveBranchId, pageable);
        } else if (branchIdFilter != null) {
            page = appointmentRepository.findByBranchId(branchIdFilter, pageable);
        } else {
            page = appointmentRepository.findAll(pageable);
        }

        return PageResponse.from(page.map(this::toAppointmentResponse));
    }

    @Transactional
    public AvailabilityExceptionResponse createException(UUID branchId,
                                                          CreateAvailabilityExceptionRequest request) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        if (availabilityExceptionRepository.existsByBranchIdAndExceptionDate(branchId, request.exceptionDate())) {
            throw new DuplicateResourceException(
                    "An availability exception already exists for this branch on " + request.exceptionDate());
        }

        BranchAvailabilityException exception = new BranchAvailabilityException();
        exception.setBranch(branch);
        exception.setExceptionDate(request.exceptionDate());
        exception.setType(request.type());
        exception.setReason(request.reason());

        return AvailabilityExceptionResponse.from(availabilityExceptionRepository.save(exception));
    }

    @Transactional(readOnly = true)
    public List<AvailabilityExceptionResponse> listExceptions(UUID branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }
        return availabilityExceptionRepository
                .findByBranchIdOrderByExceptionDateAsc(branchId)
                .stream()
                .map(AvailabilityExceptionResponse::from)
                .toList();
    }

    @Transactional
    public void deleteException(UUID branchId, UUID exceptionId) {
        BranchAvailabilityException exception = availabilityExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Exception not found: " + exceptionId));

        if (!exception.getBranch().getId().equals(branchId)) {
            throw new ResourceNotFoundException("Exception not found: " + exceptionId);
        }

        availabilityExceptionRepository.delete(exception);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page.map(AuditLogResponse::from));
    }

    @Transactional(readOnly = true)
    public AppointmentSummaryResponse getAppointmentSummary(LocalDate from, LocalDate to) {
        long pending = appointmentRepository.countByDateRangeAndStatus(from, to, AppointmentStatus.PENDING);
        long confirmed = appointmentRepository.countByDateRangeAndStatus(from, to, AppointmentStatus.CONFIRMED);
        long cancelled = appointmentRepository.countByDateRangeAndStatus(from, to, AppointmentStatus.CANCELLED);
        long completed = appointmentRepository.countByDateRangeAndStatus(from, to, AppointmentStatus.COMPLETED);
        long noShow = appointmentRepository.countByDateRangeAndStatus(from, to, AppointmentStatus.NO_SHOW);
        long rescheduled = appointmentRepository.countByDateRangeAndStatus(from, to, AppointmentStatus.RESCHEDULED);
        long totalBooked = pending + confirmed + cancelled + completed + noShow + rescheduled;

        return new AppointmentSummaryResponse(
                totalBooked, pending, confirmed, cancelled, completed, noShow, rescheduled, from, to);
    }

    @Transactional(readOnly = true)
    public BranchUtilisationResponse getBranchUtilisation(UUID branchId, LocalDate from, LocalDate to) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        List<AppointmentStatus> activeStatuses =
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                        AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW);

        long bookedSlots = appointmentRepository.countByBranchAndDateRangeAndStatuses(
                branchId, from, to, activeStatuses);

        long totalSlots = appointmentRepository.countByBranchAndDateRangeAndStatuses(
                branchId, from, to, List.of(AppointmentStatus.values()));

        double utilisation = totalSlots == 0 ? 0.0 : (double) bookedSlots / totalSlots;

        return new BranchUtilisationResponse(
                branchId, branch.getName(), from, to, totalSlots, bookedSlots, utilisation);
    }

    @Transactional(readOnly = true)
    public List<ServicePopularityResponse> getServicePopularity(LocalDate from, LocalDate to) {
        return appointmentRepository.findServicePopularity(from, to)
                .stream()
                .map(row -> new ServicePopularityResponse(
                        (UUID) row[0],
                        (String) row[1],
                        (long) row[2]
                ))
                .toList();
    }

    private AppointmentResponse toAppointmentResponse(com.capitec.capibook.appointment.Appointment a) {
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
}
