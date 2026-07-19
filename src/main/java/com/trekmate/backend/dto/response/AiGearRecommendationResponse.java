package com.trekmate.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Response từ AI gợi ý trang bị cho chuyến trekking.
 * Mỗi GearItem có cờ isAvailableForRent = true nếu có sẵn trong kho cho thuê.
 *
 * @JsonProperty bắt buộc trên Java records để Jackson serialization/deserialization
 * sang JSONB hoạt động đúng (đặc biệt khi đọc lại từ database cache).
 */
public record AiGearRecommendationResponse(
        @JsonProperty("tourTitle")       String tourTitle,
        @JsonProperty("weatherSummary")  String weatherSummary,
        @JsonProperty("overallAdvice")   String overallAdvice,
        @JsonProperty("essentials")      List<GearItem> essentials,
        @JsonProperty("recommended")     List<GearItem> recommended,
        @JsonProperty("disclaimer")      String disclaimer
) {
    public record GearItem(
            @JsonProperty("name")                 String name,
            @JsonProperty("reason")               String reason,
            @JsonProperty("category")             String category,
            @JsonProperty("isAvailableForRent")   boolean isAvailableForRent,
            @JsonProperty("equipmentId")          Long equipmentId,
            @JsonProperty("equipmentImageUrl")    String equipmentImageUrl,
            @JsonProperty("pricePerDay")          String pricePerDay
    ) {}
}
