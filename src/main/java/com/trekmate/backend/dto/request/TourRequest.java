package com.trekmate.backend.dto.request;

import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.validation.ValidTourDuration;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@ValidTourDuration
public record TourRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        @Size(max = 255, message = "Slug cannot exceed 255 characters")
        String slug,

        @Size(max = 500, message = "Short description cannot exceed 500 characters")
        String shortDescription,

        String description,

        @NotNull(message = "Difficulty level is required")
        DifficultyLevel difficulty,

        @NotNull(message = "Duration days is required")
        @Min(value = 0, message = "Duration days cannot be negative")
        Short durationDays,

        @NotNull(message = "Duration nights is required")
        @Min(value = 0, message = "Duration nights cannot be negative")
        Short durationNights,

        @DecimalMin(value = "0.0", message = "Distance cannot be negative")
        BigDecimal distanceKm,

        @Min(value = 0, message = "Max elevation cannot be negative")
        Integer maxElevationM,

        @NotBlank(message = "Start location is required")
        @Pattern(regexp = "^[\\p{L}\\s,.-]+$", message = "Start location must contain only letters, spaces, and basic punctuation")
        String startLocation,

        @NotBlank(message = "End location is required")
        @Pattern(regexp = "^[\\p{L}\\s,.-]+$", message = "End location must contain only letters, spaces, and basic punctuation")
        String endLocation,

        BigDecimal startLat,
        BigDecimal startLng,
        BigDecimal endLat,
        BigDecimal endLng,
        String routeGpxUrl,
        List<String> highlights,
        List<String> includes,
        List<String> excludes,
        List<String> requirements,
        TourStatus status
) {}

