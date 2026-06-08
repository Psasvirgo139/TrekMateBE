package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.DifficultyLevel;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TourDailyItineraryResponse(
        UUID id,
        Short dayNumber,
        String dayTitle,
        String dayDescription,
        UUID startWaypointId,
        String startWaypointName,
        UUID endWaypointId,
        String endWaypointName,
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
        String overnightWaypointName,
        String overnightNotes,
        String safetyNotes,
        String guideNotes,
        Short sortOrder,
        List<ItineraryWaypointResponse> waypointLinks
) {}
