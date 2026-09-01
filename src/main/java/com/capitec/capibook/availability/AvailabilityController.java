package com.capitec.capibook.availability;

import com.capitec.capibook.availability.dto.AvailabilityResponse;
import com.capitec.capibook.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailability(
            @RequestParam UUID branchId,
            @RequestParam UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        AvailabilityResponse response = availabilityService.getAvailability(branchId, serviceId, date);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
