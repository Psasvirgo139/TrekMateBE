package com.trekmate.backend.dto.response;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryResponse {

    private Double avgOverallRating;
    private Double avgGuideRating;
    private Double avgSceneryRating;
    private Double avgSafetyRating;
    private Double avgValueRating;
    private Double avgDifficultyRating;
    private Double avgEquipmentRating;

    private Long totalReviews;

    // Key: rating (1-5), Value: count
    private Map<Integer, Long> ratingDistribution;
}
