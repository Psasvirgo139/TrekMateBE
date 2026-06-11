package com.trekmate.backend.dto.response;

import java.time.LocalDateTime;

public record TourImageResponse(
        Long id,
        String imageUrl,
        String caption,
        String altText,
        Boolean isCover,
        Short sortOrder,
        LocalDateTime createdAt
) {}
