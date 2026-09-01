package com.capitec.capibook.branch;

import com.capitec.capibook.branch.dto.BranchResponse;
import com.capitec.capibook.branch.dto.CreateBranchRequest;
import com.capitec.capibook.branch.dto.OperatingHoursEntry;
import com.capitec.capibook.branch.dto.UpdateBranchRequest;
import com.capitec.capibook.common.ApiResponse;
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
@RequestMapping("/api/v1/branches")
@Tag(name = "Branches", description = "Branch management")
@SecurityRequirement(name = "bearerAuth")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new branch (SYSTEM_ADMIN only)")
    public ResponseEntity<ApiResponse<BranchResponse>> create(@Valid @RequestBody CreateBranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Branch created", branchService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List all active branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.ok(branchService.listActive()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a branch by ID")
    public ResponseEntity<ApiResponse<BranchResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(branchService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Update a branch (SYSTEM_ADMIN only)")
    public ResponseEntity<ApiResponse<BranchResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBranchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Branch updated", branchService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Deactivate a branch (SYSTEM_ADMIN only)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        branchService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/operating-hours")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Replace branch operating hours (SYSTEM_ADMIN only)")
    public ResponseEntity<ApiResponse<BranchResponse>> updateOperatingHours(
            @PathVariable UUID id,
            @Valid @RequestBody List<@Valid OperatingHoursEntry> hours) {
        return ResponseEntity.ok(ApiResponse.ok("Operating hours updated", branchService.updateOperatingHours(id, hours)));
    }
}
