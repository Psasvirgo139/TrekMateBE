package com.trekmate.backend.dto.response;

import com.trekmate.backend.dto.request.CertificationDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GuideProfileResponse(
        UUID userId,
        String email,
        String phone,
        String displayName,
        String avatarUrl,
        String bio,
        String homeProvince,
        Short experienceYears,
        List<String> languages,
        List<String> specializations,
        String idCardNumber,
        Boolean idCardVerified,
        BigDecimal avgRating,
        Integer totalReviews,
        Long totalToursLed,
        Boolean isAvailable,
        List<CertificationDto> certifications,
        List<GuideTourHistoryResponse> toursLedHistory
) {}
