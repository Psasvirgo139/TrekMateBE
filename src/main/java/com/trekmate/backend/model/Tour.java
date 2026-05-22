package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tours", indexes = {
        @Index(name = "idx_tours_slug",       columnList = "slug",        unique = true),
        @Index(name = "idx_tours_difficulty", columnList = "difficulty"),
        @Index(name = "idx_tours_status",     columnList = "status"),
        @Index(name = "idx_tours_rating",     columnList = "avg_rating")
})
@EntityListeners(AuditingEntityListener.class)
public class Tour extends BaseEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MODERATE;

    @Column(name = "duration_days", nullable = false)
    private Short durationDays;

    @Column(name = "duration_nights", nullable = false)
    @Builder.Default
    private Short durationNights = 0;

    @Column(name = "distance_km", precision = 7, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "max_elevation_m")
    private Integer maxElevationM;

    @Column(name = "start_location", length = 255)
    private String startLocation;

    @Column(name = "end_location", length = 255)
    private String endLocation;

    @Column(name = "start_lat", precision = 10, scale = 7)
    private BigDecimal startLat;

    @Column(name = "start_lng", precision = 10, scale = 7)
    private BigDecimal startLng;

    @Column(name = "end_lat", precision = 10, scale = 7)
    private BigDecimal endLat;

    @Column(name = "end_lng", precision = 10, scale = 7)
    private BigDecimal endLng;

    @Column(name = "route_gpx_url", columnDefinition = "TEXT")
    private String routeGpxUrl;

    // JSONB: ["Đỉnh 3147m","Rừng nguyên sinh"]
    @Column(name = "highlights", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String highlights = "[]";

    // JSONB: ["HDV chuyên nghiệp","Lều trại"]
    @Column(name = "includes", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String includes = "[]";

    @Column(name = "excludes", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String excludes = "[]";

    // JSONB: [{"type":"fitness","note":"Leo bộ 8h/ngày"}]
    @Column(name = "requirements", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String requirements = "[]";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TourStatus status = TourStatus.DRAFT;

    @Column(name = "avg_rating", precision = 3, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "total_departures", nullable = false)
    @Builder.Default
    private Integer totalDepartures = 0;

    @Column(name = "total_bookings", nullable = false)
    @Builder.Default
    private Integer totalBookings = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    // ── Relationships ─────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<TourImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<TourWaypoint> waypoints = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @Builder.Default
    private List<TourDailyItinerary> dailyItinerary = new ArrayList<>();

    @OneToMany(mappedBy = "tour", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TourDeparture> departures = new ArrayList<>();
}
