package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TourCardResponse(
        UUID id,
        String title,
        String slug,
        DifficultyLevel difficulty,
        Short durationDays,
        Short durationNights,
        BigDecimal distanceKm,
        Integer maxElevationM,
        String startLocation,
        String endLocation,
        BigDecimal avgRating,
        Integer totalReviews,
        Integer totalDepartures,
        TourStatus status,
        // Giá thấp nhất từ các departure đang mở — null nếu chưa có lịch
        BigDecimal priceFrom,
        // Số departure sắp tới (OPEN + SCHEDULED)
        long upcomingDeparturesCount,
        // Điểm nổi bật của tuyến (từ JSON highlights)
        List<String> highlights
) {}
