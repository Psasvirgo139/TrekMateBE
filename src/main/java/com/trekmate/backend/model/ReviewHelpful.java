package com.trekmate.backend.model;

import com.trekmate.backend.model.embeddable.ReviewHelpfulId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "review_helpful")
public class ReviewHelpful {

    @EmbeddedId
    private ReviewHelpfulId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reviewId")
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

