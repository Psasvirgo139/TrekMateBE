package com.trekmate.backend.controller;

import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.EquipmentCategoryResponse;
import com.trekmate.backend.dto.response.EquipmentResponse;
import com.trekmate.backend.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public equipment endpoints for customers browsing rental equipment
 * during tour booking. No authentication required.
 */
@RestController
@RequestMapping("/v1/rental/equipments")
@RequiredArgsConstructor
@Tag(name = "Public Rental Equipment", description = "Public API for browsing available rental equipment during tour booking")
public class PublicEquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    @Operation(summary = "List active rental equipment (public)",
               description = "Returns a paginated list of active equipment available for rental. Used by tour booking page.")
    public ApiResponse<Page<EquipmentResponse>> getAvailableEquipments(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Always filter by isActive=true for public access
        Page<EquipmentResponse> data = equipmentService.getEquipments(categoryId, true, page, size);
        return ApiResponse.<Page<EquipmentResponse>>builder()
                .code(200)
                .message("Get active equipments successfully")
                .data(data)
                .build();
    }

    @GetMapping("/categories")
    @Operation(summary = "List equipment categories (public)",
               description = "Returns all equipment categories for filtering rental equipment.")
    public ApiResponse<List<EquipmentCategoryResponse>> getCategories() {
        return ApiResponse.<List<EquipmentCategoryResponse>>builder()
                .code(200)
                .message("Get equipment categories successfully")
                .data(equipmentService.getCategories())
                .build();
    }
}
