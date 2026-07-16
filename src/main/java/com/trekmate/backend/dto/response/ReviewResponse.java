package com.trekmate.backend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private UUID tourId;
    private String tourTitle;
    private Long bookingId;
    private LocalDate departureDate;

    // Ratings
    private Short overallRating;
    private Short guideRating;
    private Short difficultyRating;
    private Short sceneryRating;
    private Short safetyRating;
    private Short valueRating;
    private Short equipmentRating;

    // Content
    private String title;
    private String comment;
    private List<String> photos;

    // User info
    private Boolean isAnonymous;
    private String userName;
    private String userAvatar;

    // Status
    private Boolean isApproved;
    private Boolean isFeatured;

    // Guide reply
    private String guideReply;
    private LocalDateTime guideRepliedAt;

    // Helpful
    private Integer helpfulCount;
    private Boolean isHelpfulByCurrentUser;

    private LocalDateTime createdAt;
}
