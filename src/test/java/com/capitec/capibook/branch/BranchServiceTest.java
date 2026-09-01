package com.capitec.capibook.branch;

import com.capitec.capibook.branch.dto.BranchResponse;
import com.capitec.capibook.branch.dto.CreateBranchRequest;
import com.capitec.capibook.branch.dto.OperatingHoursEntry;
import com.capitec.capibook.branch.dto.UpdateBranchRequest;
import com.capitec.capibook.exception.DuplicateResourceException;
import com.capitec.capibook.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchOperatingHoursRepository operatingHoursRepository;

    @InjectMocks
    private BranchService branchService;

    private Branch testBranch;

    @BeforeEach
    void setUp() {
        testBranch = new Branch();
        testBranch.setBranchCode("CPT001");
        testBranch.setName("Cape Town Main");
        testBranch.setAddress("1 Adderley Street");
        testBranch.setCity("Cape Town");
        testBranch.setProvince("Western Cape");
        testBranch.setPostalCode("8001");
        testBranch.setActive(true);
    }

    @Test
    void create_withNewBranchCode_returnsResponse() {
        when(branchRepository.existsByBranchCode("CPT001")).thenReturn(false);
        when(branchRepository.save(any(Branch.class))).thenReturn(testBranch);

        CreateBranchRequest request = new CreateBranchRequest(
                "CPT001", "Cape Town Main", "1 Adderley Street",
                "Cape Town", "Western Cape", "8001", null, null, null, null, null);

        BranchResponse response = branchService.create(request);

        assertThat(response.branchCode()).isEqualTo("CPT001");
        assertThat(response.name()).isEqualTo("Cape Town Main");
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void create_withDuplicateBranchCode_throwsDuplicateResourceException() {
        when(branchRepository.existsByBranchCode("CPT001")).thenReturn(true);

        CreateBranchRequest request = new CreateBranchRequest(
                "CPT001", "Cape Town Main", "1 Adderley Street",
                "Cape Town", "Western Cape", "8001", null, null, null, null, null);

        assertThatThrownBy(() -> branchService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CPT001");

        verify(branchRepository, never()).save(any());
    }

    @Test
    void getById_withExistingId_returnsBranch() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.of(testBranch));
        when(operatingHoursRepository.findByBranchId(id)).thenReturn(List.of());

        BranchResponse response = branchService.getById(id);

        assertThat(response.branchCode()).isEqualTo("CPT001");
    }

    @Test
    void getById_withUnknownId_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withValidRequest_updatesFields() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.of(testBranch));
        when(branchRepository.save(any(Branch.class))).thenReturn(testBranch);
        when(operatingHoursRepository.findByBranchId(id)).thenReturn(List.of());

        UpdateBranchRequest request = new UpdateBranchRequest(
                "Updated Name", "New Address", "Johannesburg",
                "Gauteng", "2000", null, null, null, null, null);

        branchService.update(id, request);

        assertThat(testBranch.getName()).isEqualTo("Updated Name");
        assertThat(testBranch.getCity()).isEqualTo("Johannesburg");
        verify(branchRepository).save(testBranch);
    }

    @Test
    void deactivate_setsActiveFalse() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.of(testBranch));
        when(branchRepository.save(any(Branch.class))).thenReturn(testBranch);

        branchService.deactivate(id);

        assertThat(testBranch.isActive()).isFalse();
        verify(branchRepository).save(testBranch);
    }

    @Test
    void updateOperatingHours_withValidEntries_savesHours() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.of(testBranch));

        List<OperatingHoursEntry> hours = List.of(
                new OperatingHoursEntry(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), false),
                new OperatingHoursEntry(DayOfWeek.SATURDAY, null, null, true)
        );

        branchService.updateOperatingHours(id, hours);

        verify(operatingHoursRepository).deleteByBranchId(id);
        verify(operatingHoursRepository).saveAll(anyList());
    }

    @Test
    void updateOperatingHours_withDuplicateDays_throwsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.of(testBranch));

        List<OperatingHoursEntry> hours = List.of(
                new OperatingHoursEntry(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), false),
                new OperatingHoursEntry(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), false)
        );

        assertThatThrownBy(() -> branchService.updateOperatingHours(id, hours))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate days");
    }

    @Test
    void updateOperatingHours_withMissingTimesOnOpenDay_throwsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.of(testBranch));

        List<OperatingHoursEntry> hours = List.of(
                new OperatingHoursEntry(DayOfWeek.MONDAY, null, null, false)
        );

        assertThatThrownBy(() -> branchService.updateOperatingHours(id, hours))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openTime and closeTime are required");
    }

    @Test
    void updateOperatingHours_withOpenTimeAfterCloseTime_throwsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.of(testBranch));

        List<OperatingHoursEntry> hours = List.of(
                new OperatingHoursEntry(DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(8, 0), false)
        );

        assertThatThrownBy(() -> branchService.updateOperatingHours(id, hours))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openTime must be before closeTime");
    }
}
