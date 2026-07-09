package com.trekmate.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AvailableGuideResponse(
    UUID guideId,
    String displayName,
    String phone,
    Short experienceYears,
    BigDecimal avgRating,
    String avatarUrl
) {}
