package com.trekmate.backend.mapper;

import com.trekmate.backend.dto.response.ReviewResponse;
import com.trekmate.backend.model.Customer;
import com.trekmate.backend.model.Review;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReviewMapper {

    /**
     * Convert Review entity to ReviewResponse DTO.
     * Handles anonymous name masking and helpful state for current user.
     */
    public ReviewResponse toResponse(Review review, UUID currentUserId, boolean isHelpful) {
        Customer customer = review.getUser().getCustomer();
        String userName;
        String userAvatar;

        if (Boolean.TRUE.equals(review.getIsAnonymous())) {
            userName = "Ẩn danh";
            userAvatar = null;
        } else {
            userName = customer != null ? customer.getFullName() : review.getUser().getEmail();
            userAvatar = customer != null ? customer.getAvatarUrl() : null;
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .tourId(review.getTour().getId())
                .tourTitle(review.getTour().getTitle())
                .bookingId(review.getBooking().getId())
                .departureDate(review.getDeparture().getDepartureDate())
                .overallRating(review.getOverallRating())
                .guideRating(review.getGuideRating())
                .difficultyRating(review.getDifficultyRating())
                .sceneryRating(review.getSceneryRating())
                .safetyRating(review.getSafetyRating())
                .valueRating(review.getValueRating())
                .equipmentRating(review.getEquipmentRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .photos(review.getPhotos())
                .isAnonymous(review.getIsAnonymous())
                .userName(userName)
                .userAvatar(userAvatar)
                .isApproved(review.getIsApproved())
                .isFeatured(review.getIsFeatured())
                .guideReply(review.getGuideReply())
                .guideRepliedAt(review.getGuideRepliedAt())
                .helpfulCount(review.getHelpfulCount())
                .isHelpfulByCurrentUser(isHelpful)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
