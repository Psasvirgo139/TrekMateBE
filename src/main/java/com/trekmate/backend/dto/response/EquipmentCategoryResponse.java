package com.trekmate.backend.dto.response;

public record EquipmentCategoryResponse(
        Long id,
        String name,
        String slug,
        String icon,
        Short sortOrder,
        Boolean isActive,
        long equipmentCount
) {}
