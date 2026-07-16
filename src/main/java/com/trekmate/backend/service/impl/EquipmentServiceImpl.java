package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.EquipmentCategoryRequest;
import com.trekmate.backend.dto.request.EquipmentRequest;
import com.trekmate.backend.dto.request.ReturnEquipmentRequest;
import com.trekmate.backend.dto.response.EquipmentCategoryResponse;
import com.trekmate.backend.dto.response.EquipmentRentalResponse;
import com.trekmate.backend.dto.response.EquipmentResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Equipment;
import com.trekmate.backend.model.EquipmentCategory;
import com.trekmate.backend.model.EquipmentRental;
import com.trekmate.backend.model.enums.EquipmentCondition;
import com.trekmate.backend.repository.EquipmentCategoryRepository;
import com.trekmate.backend.repository.EquipmentRentalRepository;
import com.trekmate.backend.repository.EquipmentRepository;
import com.trekmate.backend.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentCategoryRepository categoryRepository;
    private final EquipmentRentalRepository rentalRepository;

    // ─── Categories ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentCategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("sortOrder", "name"))
                .stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EquipmentCategoryResponse createCategory(EquipmentCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Slug '" + request.slug() + "' already exists");
        }
        EquipmentCategory cat = categoryRepository.save(EquipmentCategory.builder()
                .name(request.name())
                .slug(request.slug())
                .icon(request.icon())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : (short) 0)
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build());
        return toCategoryResponse(cat);
    }

    @Override
    @Transactional
    public EquipmentCategoryResponse updateCategory(Long id, EquipmentCategoryRequest request) {
        EquipmentCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_CATEGORY_NOT_FOUND));

        // Check slug uniqueness only if changed
        if (!cat.getSlug().equals(request.slug()) && categoryRepository.existsBySlug(request.slug())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Slug '" + request.slug() + "' already exists");
        }

        cat.setName(request.name());
        cat.setSlug(request.slug());
        if (StringUtils.hasText(request.icon())) cat.setIcon(request.icon());
        if (request.sortOrder() != null) cat.setSortOrder(request.sortOrder());
        if (request.isActive() != null) cat.setIsActive(request.isActive());

        return toCategoryResponse(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.EQUIPMENT_CATEGORY_NOT_FOUND);
        }
        if (equipmentRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.EQUIPMENT_CATEGORY_HAS_ITEMS);
        }
        categoryRepository.deleteById(id);
    }

    // ─── Equipment ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<EquipmentResponse> getEquipments(Long categoryId, Boolean isActive, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by("id").descending());

        if (categoryId != null && isActive != null) {
            return equipmentRepository.findByCategoryIdAndIsActive(categoryId, isActive, pageRequest)
                    .map(this::toEquipmentResponse);
        }
        if (categoryId != null) {
            return equipmentRepository.findByCategoryId(categoryId, pageRequest)
                    .map(this::toEquipmentResponse);
        }
        if (isActive != null) {
            return equipmentRepository.findByIsActive(isActive, pageRequest)
                    .map(this::toEquipmentResponse);
        }
        return equipmentRepository.findAll(pageRequest).map(this::toEquipmentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentResponse getEquipment(Long id) {
        return equipmentRepository.findById(id)
                .map(this::toEquipmentResponse)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }

    @Override
    @Transactional
    public EquipmentResponse createEquipment(EquipmentRequest request) {
        EquipmentCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_CATEGORY_NOT_FOUND));

        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .category(category)
                .name(request.name())
                .description(request.description())
                .brand(request.brand())
                .model(request.model())
                .pricePerDay(request.pricePerDay())
                .depositAmount(request.depositAmount() != null ? request.depositAmount() : BigDecimal.ZERO)
                .totalStock(request.totalStock())
                .availableStock(request.availableStock() != null ? request.availableStock() : request.totalStock())
                .condition(parseCondition(request.condition()))
                .imageUrl(request.imageUrl())
                .weightKg(request.weightKg())
                .specifications(request.specifications() != null ? request.specifications() : new HashMap<>())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build());

        return toEquipmentResponse(equipment);
    }

    @Override
    @Transactional
    public EquipmentResponse updateEquipment(Long id, EquipmentRequest request) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));

        EquipmentCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_CATEGORY_NOT_FOUND));

        equipment.setCategory(category);
        equipment.setName(request.name());
        if (request.description() != null) equipment.setDescription(request.description());
        if (request.brand() != null) equipment.setBrand(request.brand());
        if (request.model() != null) equipment.setModel(request.model());
        equipment.setPricePerDay(request.pricePerDay());
        if (request.depositAmount() != null) equipment.setDepositAmount(request.depositAmount());
        equipment.setTotalStock(request.totalStock());
        if (request.availableStock() != null) equipment.setAvailableStock(request.availableStock());
        if (request.condition() != null) equipment.setCondition(parseCondition(request.condition()));
        if (request.imageUrl() != null) equipment.setImageUrl(request.imageUrl());
        if (request.weightKg() != null) equipment.setWeightKg(request.weightKg());
        if (request.specifications() != null) equipment.setSpecifications(request.specifications());
        if (request.isActive() != null) equipment.setIsActive(request.isActive());

        return toEquipmentResponse(equipmentRepository.save(equipment));
    }

    @Override
    @Transactional
    public EquipmentResponse toggleActive(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));
        equipment.setIsActive(!Boolean.TRUE.equals(equipment.getIsActive()));
        return toEquipmentResponse(equipmentRepository.save(equipment));
    }

    @Override
    @Transactional
    public void deleteEquipment(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));

        // Soft delete if there are active (not returned) rentals
        if (rentalRepository.existsByEquipmentIdAndReturnedAtIsNull(id)) {
            equipment.setIsActive(false);
            equipmentRepository.save(equipment);
        } else {
            equipmentRepository.delete(equipment);
        }
    }

    // ─── Rentals ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<EquipmentRentalResponse> getRentals(Long equipmentId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());

        if (equipmentId != null) {
            return rentalRepository.findByEquipmentId(equipmentId, pageRequest)
                    .map(this::toRentalResponse);
        }
        return rentalRepository.findAll(pageRequest).map(this::toRentalResponse);
    }

    @Override
    @Transactional
    public EquipmentRentalResponse returnEquipment(Long rentalId, ReturnEquipmentRequest request) {
        EquipmentRental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_RENTAL_NOT_FOUND));

        if (rental.getReturnedAt() != null) {
            throw new AppException(ErrorCode.EQUIPMENT_ALREADY_RETURNED);
        }

        rental.setReturnedAt(LocalDateTime.now());
        if (StringUtils.hasText(request.returnCondition())) {
            rental.setReturnCondition(parseCondition(request.returnCondition()));
        }
        if (request.damageFee() != null) {
            rental.setDamageFee(request.damageFee());
        }
        if (StringUtils.hasText(request.notes())) {
            rental.setNotes(request.notes());
        }

        // Restore stock
        Equipment equipment = rental.getEquipment();
        short restored = (short) (equipment.getAvailableStock() + rental.getQuantity());
        equipment.setAvailableStock(restored);
        equipmentRepository.save(equipment);

        return toRentalResponse(rentalRepository.save(rental));
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private EquipmentCategoryResponse toCategoryResponse(EquipmentCategory cat) {
        long count = equipmentRepository.existsByCategoryId(cat.getId())
                ? cat.getEquipmentList().size() : 0;
        return new EquipmentCategoryResponse(
                cat.getId(), cat.getName(), cat.getSlug(), cat.getIcon(),
                cat.getSortOrder(), cat.getIsActive(),
                cat.getEquipmentList() != null ? cat.getEquipmentList().size() : 0
        );
    }

    private EquipmentResponse toEquipmentResponse(Equipment e) {
        return new EquipmentResponse(
                e.getId(),
                e.getCategory() != null ? e.getCategory().getId() : null,
                e.getCategory() != null ? e.getCategory().getName() : null,
                e.getCategory() != null ? e.getCategory().getSlug() : null,
                e.getName(), e.getDescription(), e.getBrand(), e.getModel(),
                e.getPricePerDay(), e.getDepositAmount(),
                e.getTotalStock(), e.getAvailableStock(),
                e.getCondition() != null ? e.getCondition().name() : null,
                e.getImageUrl(), e.getWeightKg(), e.getSpecifications(), e.getIsActive()
        );
    }

    private EquipmentRentalResponse toRentalResponse(EquipmentRental r) {
        String customerName = null;
        String bookingCode = null;
        if (r.getBooking() != null) {
            bookingCode = r.getBooking().getBookingCode();
            if (r.getBooking().getUser() != null && r.getBooking().getUser().getCustomer() != null) {
                customerName = r.getBooking().getUser().getCustomer().getFullName();
            } else if (r.getBooking().getUser() != null) {
                customerName = r.getBooking().getUser().getEmail();
            }
        }
        return new EquipmentRentalResponse(
                r.getId(),
                r.getBooking() != null ? r.getBooking().getId() : null,
                bookingCode,
                customerName,
                r.getEquipment() != null ? r.getEquipment().getId() : null,
                r.getEquipment() != null ? r.getEquipment().getName() : null,
                r.getQuantity(), r.getRentalDays(), r.getPricePerDay(), r.getSubtotal(),
                r.getReturnedAt(),
                r.getReturnCondition() != null ? r.getReturnCondition().name() : null,
                r.getDamageFee(), r.getNotes(), r.getCreatedAt(),
                r.getReturnedAt() != null
        );
    }

    private EquipmentCondition parseCondition(String condition) {
        if (condition == null) return EquipmentCondition.GOOD;
        try {
            return EquipmentCondition.valueOf(condition.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EquipmentCondition.GOOD;
        }
    }
}
