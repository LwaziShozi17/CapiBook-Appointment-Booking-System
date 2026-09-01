package com.capitec.capibook.servicecatalog;

import com.capitec.capibook.common.ApiResponse;
import com.capitec.capibook.servicecatalog.dto.BankingServiceResponse;
import com.capitec.capibook.servicecatalog.dto.CreateServiceRequest;
import com.capitec.capibook.servicecatalog.dto.UpdateServiceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Services", description = "Banking service catalogue management")
@SecurityRequirement(name = "bearerAuth")
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new banking service (SYSTEM_ADMIN only)")
    public ResponseEntity<ApiResponse<BankingServiceResponse>> create(@Valid @RequestBody CreateServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Service created", serviceCatalogService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List all active banking services")
    public ResponseEntity<ApiResponse<List<BankingServiceResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.ok(serviceCatalogService.listActive()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a banking service by ID")
    public ResponseEntity<ApiResponse<BankingServiceResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(serviceCatalogService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Update a banking service (SYSTEM_ADMIN only)")
    public ResponseEntity<ApiResponse<BankingServiceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Service updated", serviceCatalogService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Deactivate a banking service (SYSTEM_ADMIN only)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        serviceCatalogService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
