package com.capitec.capibook.availability;

import com.capitec.capibook.availability.dto.AvailabilityResponse;
import com.capitec.capibook.availability.dto.SlotResponse;
import com.capitec.capibook.exception.GlobalExceptionHandler;
import com.capitec.capibook.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private AvailabilityController availabilityController;

    private MockMvc mockMvc;
    private UUID branchId;
    private UUID serviceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(availabilityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        branchId = UUID.randomUUID();
        serviceId = UUID.randomUUID();
    }

    @Test
    void getAvailability_withValidParams_returns200() throws Exception {
        LocalDate date = LocalDate.of(2025, 6, 9);
        List<SlotResponse> slots = List.of(
                new SlotResponse(LocalTime.of(8, 0), LocalTime.of(8, 30), SlotStatus.AVAILABLE),
                new SlotResponse(LocalTime.of(8, 30), LocalTime.of(9, 0), SlotStatus.BOOKED)
        );
        AvailabilityResponse response = new AvailabilityResponse(branchId, serviceId, date, slots);

        when(availabilityService.getAvailability(branchId, serviceId, date)).thenReturn(response);

        mockMvc.perform(get("/api/v1/availability")
                        .param("branchId", branchId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2025-06-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slots").isArray())
                .andExpect(jsonPath("$.data.slots.length()").value(2))
                .andExpect(jsonPath("$.data.slots[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.slots[1].status").value("BOOKED"));
    }

    @Test
    void getAvailability_forHolidayOrClosedDay_returnsEmptySlots() throws Exception {
        LocalDate holiday = LocalDate.of(2025, 12, 25);
        AvailabilityResponse response = new AvailabilityResponse(branchId, serviceId, holiday, List.of());

        when(availabilityService.getAvailability(branchId, serviceId, holiday)).thenReturn(response);

        mockMvc.perform(get("/api/v1/availability")
                        .param("branchId", branchId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2025-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots").isEmpty());
    }

    @Test
    void getAvailability_withUnknownBranch_returns404() throws Exception {
        LocalDate date = LocalDate.of(2025, 6, 9);
        when(availabilityService.getAvailability(branchId, serviceId, date))
                .thenThrow(new ResourceNotFoundException("Branch not found: " + branchId));

        mockMvc.perform(get("/api/v1/availability")
                        .param("branchId", branchId.toString())
                        .param("serviceId", serviceId.toString())
                        .param("date", "2025-06-09"))
                .andExpect(status().isNotFound());
    }
}
