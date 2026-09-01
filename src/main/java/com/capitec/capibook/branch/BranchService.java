package com.capitec.capibook.branch;

import com.capitec.capibook.branch.dto.BranchResponse;
import com.capitec.capibook.branch.dto.CreateBranchRequest;
import com.capitec.capibook.branch.dto.OperatingHoursEntry;
import com.capitec.capibook.branch.dto.UpdateBranchRequest;
import com.capitec.capibook.exception.DuplicateResourceException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchOperatingHoursRepository operatingHoursRepository;

    public BranchService(BranchRepository branchRepository,
                         BranchOperatingHoursRepository operatingHoursRepository) {
        this.branchRepository = branchRepository;
        this.operatingHoursRepository = operatingHoursRepository;
    }

    public BranchResponse create(CreateBranchRequest request) {
        if (branchRepository.existsByBranchCode(request.branchCode())) {
            throw new DuplicateResourceException("Branch code already exists: " + request.branchCode());
        }
        Branch branch = new Branch();
        branch.setBranchCode(request.branchCode());
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setCity(request.city());
        branch.setProvince(request.province());
        branch.setPostalCode(request.postalCode());
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());
        branch.setPhoneNumber(request.phoneNumber());
        branch.setEmail(request.email());
        if (request.maxConcurrentAppointments() != null) {
            branch.setMaxConcurrentAppointments(request.maxConcurrentAppointments());
        }
        Branch saved = branchRepository.save(branch);
        return BranchResponse.from(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> listActive() {
        return branchRepository.findAllByActiveTrue().stream()
                .map(branch -> BranchResponse.from(branch, operatingHoursRepository.findByBranchId(branch.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getById(UUID id) {
        Branch branch = findBranchById(id);
        return BranchResponse.from(branch, operatingHoursRepository.findByBranchId(id));
    }

    public BranchResponse update(UUID id, UpdateBranchRequest request) {
        Branch branch = findBranchById(id);
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setCity(request.city());
        branch.setProvince(request.province());
        branch.setPostalCode(request.postalCode());
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());
        branch.setPhoneNumber(request.phoneNumber());
        branch.setEmail(request.email());
        if (request.maxConcurrentAppointments() != null) {
            branch.setMaxConcurrentAppointments(request.maxConcurrentAppointments());
        }
        Branch saved = branchRepository.save(branch);
        return BranchResponse.from(saved, operatingHoursRepository.findByBranchId(id));
    }

    public void deactivate(UUID id) {
        Branch branch = findBranchById(id);
        branch.setActive(false);
        branchRepository.save(branch);
    }

    public BranchResponse updateOperatingHours(UUID id, List<OperatingHoursEntry> entries) {
        Branch branch = findBranchById(id);
        validateOperatingHoursEntries(entries);
        operatingHoursRepository.deleteByBranchId(id);
        List<BranchOperatingHours> hours = entries.stream().map(entry -> {
            BranchOperatingHours h = new BranchOperatingHours();
            h.setBranch(branch);
            h.setDayOfWeek(entry.dayOfWeek());
            h.setClosed(entry.closed());
            h.setOpenTime(entry.openTime());
            h.setCloseTime(entry.closeTime());
            return h;
        }).toList();
        operatingHoursRepository.saveAll(hours);
        return BranchResponse.from(branch, hours);
    }

    private Branch findBranchById(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
    }

    private void validateOperatingHoursEntries(List<OperatingHoursEntry> entries) {
        long distinctDays = entries.stream().map(OperatingHoursEntry::dayOfWeek).distinct().count();
        if (distinctDays != entries.size()) {
            throw new IllegalArgumentException("Duplicate days of week in operating hours");
        }
        for (OperatingHoursEntry entry : entries) {
            if (!entry.closed()) {
                if (entry.openTime() == null || entry.closeTime() == null) {
                    throw new IllegalArgumentException(
                            "openTime and closeTime are required when branch is not closed on " + entry.dayOfWeek());
                }
                if (!entry.openTime().isBefore(entry.closeTime())) {
                    throw new IllegalArgumentException(
                            "openTime must be before closeTime on " + entry.dayOfWeek());
                }
            }
        }
    }
}
