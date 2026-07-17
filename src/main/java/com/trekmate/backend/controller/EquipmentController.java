package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.EquipmentCategoryRequest;
import com.trekmate.backend.dto.request.EquipmentRequest;
import com.trekmate.backend.dto.request.ReturnEquipmentRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.EquipmentCategoryResponse;
import com.trekmate.backend.dto.response.EquipmentRentalResponse;
import com.trekmate.backend.dto.response.EquipmentResponse;
import com.trekmate.backend.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Equipment management — CRUD for equipment, categories, and rental return.
 * Requires JWT + ADMIN or GUIDE role.
 */
@RestController
@RequestMapping("/admin/equipment")
@RequiredArgsConstructor
@Tag(name = "Admin Equipment", description = "Equipment management (Guide Dashboard)")
public class EquipmentController {

    private final EquipmentService equipmentService;

    // ─── Categories ───────────────────────────────────────────────────────────

    @GetMapping("/categories")
    @Operation(summary = "List all equipment categories")
    public ApiResponse<List<EquipmentCategoryResponse>> getCategories() {
        return ApiResponse.<List<EquipmentCategoryResponse>>builder()
                .code(200)
                .message("List categories successfully")
                .data(equipmentService.getCategories())
                .build();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new category")
    public ApiResponse<EquipmentCategoryResponse> createCategory(
            @Valid @RequestBody EquipmentCategoryRequest request) {
        return ApiResponse.<EquipmentCategoryResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Category created successfully")
                .data(equipmentService.createCategory(request))
                .build();
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update an equipment category")
    public ApiResponse<EquipmentCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentCategoryRequest request) {
        return ApiResponse.<EquipmentCategoryResponse>builder()
                .code(200)
                .message("Category updated successfully")
                .data(equipmentService.updateCategory(id, request))
                .build();
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete a category (only if no equipment linked)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        equipmentService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Equipment ────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Paginated equipment list, filter by categoryId and/or isActive")
    public ApiResponse<Page<EquipmentResponse>> getEquipments(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<EquipmentResponse>>builder()
                .code(200)
                .message("Get equipments successfully")
                .data(equipmentService.getEquipments(categoryId, isActive, page, size))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment by id")
    public ApiResponse<EquipmentResponse> getEquipment(@PathVariable Long id) {
        return ApiResponse.<EquipmentResponse>builder()
                .code(200)
                .message("Get equipment details successfully")
                .data(equipmentService.getEquipment(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new equipment")
    public ApiResponse<EquipmentResponse> createEquipment(
            @Valid @RequestBody EquipmentRequest request) {
        return ApiResponse.<EquipmentResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Equipment created successfully")
                .data(equipmentService.createEquipment(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update equipment")
    public ApiResponse<EquipmentResponse> updateEquipment(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentRequest request) {
        return ApiResponse.<EquipmentResponse>builder()
                .code(200)
                .message("Equipment updated successfully")
                .data(equipmentService.updateEquipment(id, request))
                .build();
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle equipment active/inactive")
    public ApiResponse<EquipmentResponse> toggleActive(@PathVariable Long id) {
        return ApiResponse.<EquipmentResponse>builder()
                .code(200)
                .message("Equipment status toggled successfully")
                .data(equipmentService.toggleActive(id))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete equipment (soft-delete if active rentals exist)")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Rentals ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/rentals")
    @Operation(summary = "List rentals for a specific equipment")
    public ApiResponse<Page<EquipmentRentalResponse>> getRentals(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<EquipmentRentalResponse>>builder()
                .code(200)
                .message("Get rentals successfully")
                .data(equipmentService.getRentals(id, page, size))
                .build();
    }

    @GetMapping("/rentals")
    @Operation(summary = "List all rentals (paginated)")
    public ApiResponse<Page<EquipmentRentalResponse>> getAllRentals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<EquipmentRentalResponse>>builder()
                .code(200)
                .message("Get all rentals successfully")
                .data(equipmentService.getRentals(null, page, size))
                .build();
    }

    @PatchMapping("/rentals/{rentalId}/return")
    @Operation(summary = "Mark a rental as returned")
    public ApiResponse<EquipmentRentalResponse> returnEquipment(
            @PathVariable Long rentalId,
            @Valid @RequestBody ReturnEquipmentRequest request) {
        return ApiResponse.<EquipmentRentalResponse>builder()
                .code(200)
                .message("Equipment returned successfully")
                .data(equipmentService.returnEquipment(rentalId, request))
                .build();
    }
}
