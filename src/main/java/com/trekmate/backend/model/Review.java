package com.trekmate.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_tour",      columnList = "tour_id, is_approved"),
        @Index(name = "idx_reviews_departure", columnList = "departure_id"),
        @Index(name = "idx_reviews_guide",     columnList = "guide_id, is_approved"),
        @Index(name = "idx_reviews_user",      columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private TourDeparture departure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private Guide guide;

    @Column(name = "overall_rating", nullable = false)
    private Short overallRating;

    @Column(name = "guide_rating")
    private Short guideRating;

    @Column(name = "difficulty_rating")
    private Short difficultyRating;

    @Column(name = "scenery_rating")
    private Short sceneryRating;

    @Column(name = "safety_rating")
    private Short safetyRating;

    @Column(name = "value_rating")
    private Short valueRating;

    @Column(name = "equipment_rating")
    private Short equipmentRating;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    // JSONB: ["url1","url2"]
    @Column(name = "photos", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String photos = "[]";

    @Column(name = "is_anonymous", nullable = false)
    @Builder.Default
    private Boolean isAnonymous = false;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "guide_reply", columnDefinition = "TEXT")
    private String guideReply;

    @Column(name = "guide_replied_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime guideRepliedAt;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;
}

