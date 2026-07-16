package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.EquipmentCategoryRequest;
import com.trekmate.backend.dto.request.EquipmentRequest;
import com.trekmate.backend.dto.request.ReturnEquipmentRequest;
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
    public ResponseEntity<List<EquipmentCategoryResponse>> getCategories() {
        return ResponseEntity.ok(equipmentService.getCategories());
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new category")
    public ResponseEntity<EquipmentCategoryResponse> createCategory(
            @Valid @RequestBody EquipmentCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update an equipment category")
    public ResponseEntity<EquipmentCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentCategoryRequest request) {
        return ResponseEntity.ok(equipmentService.updateCategory(id, request));
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
    public ResponseEntity<Page<EquipmentResponse>> getEquipments(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(equipmentService.getEquipments(categoryId, isActive, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment by id")
    public ResponseEntity<EquipmentResponse> getEquipment(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getEquipment(id));
    }

    @PostMapping
    @Operation(summary = "Create new equipment")
    public ResponseEntity<EquipmentResponse> createEquipment(
            @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.createEquipment(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update equipment")
    public ResponseEntity<EquipmentResponse> updateEquipment(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.updateEquipment(id, request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle equipment active/inactive")
    public ResponseEntity<EquipmentResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.toggleActive(id));
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
    public ResponseEntity<Page<EquipmentRentalResponse>> getRentals(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(equipmentService.getRentals(id, page, size));
    }

    @GetMapping("/rentals")
    @Operation(summary = "List all rentals (paginated)")
    public ResponseEntity<Page<EquipmentRentalResponse>> getAllRentals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(equipmentService.getRentals(null, page, size));
    }

    @PatchMapping("/rentals/{rentalId}/return")
    @Operation(summary = "Mark a rental as returned")
    public ResponseEntity<EquipmentRentalResponse> returnEquipment(
            @PathVariable Long rentalId,
            @Valid @RequestBody ReturnEquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.returnEquipment(rentalId, request));
    }
}
