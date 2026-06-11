package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TourDetailResponse(
        UUID id,
        String title,
        String slug,
        String shortDescription,
        String description,
        DifficultyLevel difficulty,
        Short durationDays,
        Short durationNights,
        BigDecimal distanceKm,
        Integer maxElevationM,
        String startLocation,
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
        TourStatus status,
        BigDecimal avgRating,
        Integer totalReviews,
        Integer totalDepartures,
        Integer totalBookings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TourImageResponse> images,
        List<TourWaypointResponse> waypoints,
        List<TourDailyItineraryResponse> dailyItinerary
) {}
