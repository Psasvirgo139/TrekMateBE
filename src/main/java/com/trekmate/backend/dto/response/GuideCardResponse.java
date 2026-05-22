package com.trekmate.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GuideCardResponse(
        UUID guideId,
        String displayName,
        String avatarUrl,
        String homeProvince,
        Short experienceYears,
        BigDecimal avgRating,
        Integer totalReviews,
        Integer totalToursLed,
        Boolean isAvailable,
        // Ngôn ngữ HDV biết (từ JSON)
        List<String> languages,
        // Chuyên môn (từ JSON)
        List<String> specializations,
        // Số departure sắp tới mà HDV này dẫn
        long upcomingToursCount
) {}
