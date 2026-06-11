package com.trekmate.backend.dto.request;

import com.trekmate.backend.model.enums.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TourDailyItineraryRequest(
        @NotNull(message = "Day number is required")
        Short dayNumber,

        @NotBlank(message = "Day title cannot be blank")
        @Size(max = 200, message = "Day title cannot exceed 200 characters")
        String dayTitle,

        String dayDescription,
        UUID startWaypointId,
        UUID endWaypointId,
        BigDecimal distanceKm,
        Integer elevationGainM,
        Integer elevationLossM,
        BigDecimal walkingHoursMin,
        BigDecimal walkingHoursMax,
        DifficultyLevel dayDifficulty,
        LocalTime suggestedStartTime,
        LocalTime suggestedEndTime,
        List<Map<String, Object>> mealsIncluded,
        String mealNotes,
        UUID overnightWaypointId,
        String overnightNotes,
        String safetyNotes,
        String guideNotes,
        Short sortOrder,
        List<ItineraryWaypointRequest> waypointLinks
) {}
