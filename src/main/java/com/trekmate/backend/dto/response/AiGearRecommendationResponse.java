package com.trekmate.backend.dto.response;

import java.util.List;

/**
 * Response từ AI gợi ý trang bị cho chuyến trekking.
 * Mỗi GearItem có cờ isAvailableForRent = true nếu có sẵn trong kho cho thuê.
 */
public record AiGearRecommendationResponse(
        String tourTitle,
        String weatherSummary,
        String overallAdvice,
        List<GearItem> essentials,
        List<GearItem> recommended,
        String disclaimer
) {
    public record GearItem(
            String name,
            String reason,
            String category,
            boolean isAvailableForRent,
            Long equipmentId,
            String equipmentImageUrl,
            String pricePerDay
    ) {}
}
