package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record ReturnEquipmentRequest(
        String returnCondition,
        @DecimalMin("0.0") BigDecimal damageFee,
        String notes
) {}
