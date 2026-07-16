package com.trekmate.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EquipmentRentalResponse(
        Long id,
        Long bookingId,
        String bookingCode,
        String customerName,
        Long equipmentId,
        String equipmentName,
        Short quantity,
        Short rentalDays,
        BigDecimal pricePerDay,
        BigDecimal subtotal,
        LocalDateTime returnedAt,
        String returnCondition,
        BigDecimal damageFee,
        String notes,
        LocalDateTime createdAt,
        boolean returned
) {}
