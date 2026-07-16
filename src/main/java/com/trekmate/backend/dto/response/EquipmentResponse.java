package com.trekmate.backend.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record EquipmentResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String name,
        String description,
        String brand,
        String model,
        BigDecimal pricePerDay,
        BigDecimal depositAmount,
        Short totalStock,
        Short availableStock,
        String condition,
        String imageUrl,
        BigDecimal weightKg,
        Map<String, Object> specifications,
        Boolean isActive
) {}
