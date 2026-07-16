package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BulkDepartureRequest(
        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotEmpty(message = "Days of week list cannot be empty")
        List<DayOfWeek> daysOfWeek,

        @NotNull(message = "Price per person is required")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        BigDecimal pricePerPerson,

        @NotNull(message = "Max group size is required")
        @Min(value = 1, message = "Max group size must be at least 1")
        Short maxGroupSize,

        @Min(value = 1, message = "Min group size must be at least 1")
        Short minGroupSize,

        Boolean allowJoinTour,

        String meetingPoint,

        BigDecimal meetingLat,

        BigDecimal meetingLng,

        String notes,

        List<UUID> guideIds
) {}
