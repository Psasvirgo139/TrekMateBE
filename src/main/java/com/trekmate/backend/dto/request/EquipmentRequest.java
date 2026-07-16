package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

public record EquipmentRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String name,
        String description,
        @Size(max = 100) String brand,
        @Size(max = 100) String model,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerDay,
        @DecimalMin("0.0") BigDecimal depositAmount,
        @NotNull @Min(0) Short totalStock,
        @NotNull @Min(0) Short availableStock,
        String condition,
        String imageUrl,
        BigDecimal weightKg,
        Map<String, Object> specifications,
        Boolean isActive
) {}
