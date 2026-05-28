package com.trekmate.backend.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "guides", indexes = {
        @Index(name = "idx_guides_user_id",    columnList = "user_id"),
        @Index(name = "idx_guides_available",  columnList = "is_available"),
        @Index(name = "idx_guides_rating",     columnList = "avg_rating"),
        @Index(name = "idx_guides_experience", columnList = "experience_years")
})
@EntityListeners(AuditingEntityListener.class)
public class Guide extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "home_province", length = 100)
    private String homeProvince;

    @Column(name = "experience_years", nullable = false)
    @Builder.Default
    private Short experienceYears = 0;

    // JSONB: [{"name":"...","issued_by":"...","year":2021}]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "certifications", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> certifications = new ArrayList<>();

    // JSONB: ["vi","en"]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> languages = new ArrayList<>();

    // JSONB: ["high-altitude","survival"]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specializations", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> specializations = new ArrayList<>();

    @Column(name = "id_card_number", unique = true, length = 20)
    private String idCardNumber;

    @Column(name = "id_card_verified", nullable = false)
    @Builder.Default
    private Boolean idCardVerified = false;

    @Column(name = "avg_rating", precision = 3, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "total_tours_led", nullable = false)
    @Builder.Default
    private Integer totalToursLed = 0;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "profile_approved_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime profileApprovedAt;

    @OneToMany(mappedBy = "guide", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DepartureGuide> departureGuides = new ArrayList<>();
}

