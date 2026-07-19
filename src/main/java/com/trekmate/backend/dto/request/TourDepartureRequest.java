package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TourDepartureRequest(
        @NotNull(message = "Departure date is required")
        LocalDate departureDate,

        LocalDate returnDate,

        LocalDate cutoffDate,

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

        java.util.List<java.util.UUID> guideIds,

        String status
) {}
