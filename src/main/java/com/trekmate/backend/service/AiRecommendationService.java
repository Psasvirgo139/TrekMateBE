package com.trekmate.backend.service;

import com.trekmate.backend.dto.response.AiGearRecommendationResponse;

import java.util.UUID;

/**
 * Service gọi Gemini AI để gợi ý trang bị trekking phù hợp
 * dựa trên thông tin tour và dự báo thời tiết.
 */
public interface AiRecommendationService {

    /**
     * Gợi ý trang bị trekking cho một departure cụ thể.
     * AI sẽ phân tích điều kiện thời tiết và thông tin tour,
     * sau đó đối chiếu với thiết bị có sẵn trong kho cho thuê.
     *
     * @param departureId UUID của TourDeparture
     * @return AiGearRecommendationResponse với danh sách trang bị và highlight available rentals
     */
    AiGearRecommendationResponse getGearRecommendation(UUID departureId);
}
