package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TourImageRequest(
        @NotBlank(message = "Image URL is required")
        String imageUrl,

        String caption,
        String altText,
        Boolean isCover,
        Short sortOrder
) {}
