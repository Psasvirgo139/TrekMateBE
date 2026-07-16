package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.EquipmentCategoryRequest;
import com.trekmate.backend.dto.request.EquipmentRequest;
import com.trekmate.backend.dto.request.ReturnEquipmentRequest;
import com.trekmate.backend.dto.response.EquipmentCategoryResponse;
import com.trekmate.backend.dto.response.EquipmentRentalResponse;
import com.trekmate.backend.dto.response.EquipmentResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EquipmentService {

    // ─── Categories ───────────────────────────────────────────────────────────
    List<EquipmentCategoryResponse> getCategories();

    EquipmentCategoryResponse createCategory(EquipmentCategoryRequest request);

    EquipmentCategoryResponse updateCategory(Long id, EquipmentCategoryRequest request);

    void deleteCategory(Long id);

    // ─── Equipment ────────────────────────────────────────────────────────────
    Page<EquipmentResponse> getEquipments(Long categoryId, Boolean isActive, int page, int size);

    EquipmentResponse getEquipment(Long id);

    EquipmentResponse createEquipment(EquipmentRequest request);

    EquipmentResponse updateEquipment(Long id, EquipmentRequest request);

    EquipmentResponse toggleActive(Long id);

    void deleteEquipment(Long id);

    // ─── Rentals ──────────────────────────────────────────────────────────────
    Page<EquipmentRentalResponse> getRentals(Long equipmentId, int page, int size);

    EquipmentRentalResponse returnEquipment(Long rentalId, ReturnEquipmentRequest request);
}
