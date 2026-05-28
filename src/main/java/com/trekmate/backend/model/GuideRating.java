package com.trekmate.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "guide_ratings", indexes = {
        @Index(name = "idx_guide_ratings_guide", columnList = "guide_id, is_approved"),
        @Index(name = "idx_guide_ratings_user",  columnList = "user_id")
},
       uniqueConstraints = @UniqueConstraint(
               name = "uq_guide_rating_per_review",
               columnNames = {"guide_id", "review_id"}
       ))
public class GuideRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @Column(name = "skill_rating", nullable = false)
    private Short skillRating;

    @Column(name = "communication_rating", nullable = false)
    private Short communicationRating;

    @Column(name = "safety_rating", nullable = false)
    private Short safetyRating;

    @Column(name = "local_knowledge_rating")
    private Short localKnowledgeRating;

    @Column(name = "punctuality_rating")
    private Short punctualityRating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}



