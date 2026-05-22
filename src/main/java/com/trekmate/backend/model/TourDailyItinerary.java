package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.DifficultyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tour_daily_itinerary",
       indexes = @Index(name = "idx_itinerary_tour", columnList = "tour_id, day_number"),
       uniqueConstraints = @UniqueConstraint(
               name = "uq_itinerary_day",
               columnNames = {"tour_id", "day_number"}
       ))
@EntityListeners(AuditingEntityListener.class)
public class TourDailyItinerary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "day_number", nullable = false)
    private Short dayNumber;

    @Column(name = "day_title", nullable = false, length = 200)
    private String dayTitle;

    @Column(name = "day_description", columnDefinition = "TEXT")
    private String dayDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_waypoint_id")
    private TourWaypoint startWaypoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_waypoint_id")
    private TourWaypoint endWaypoint;

    @Column(name = "distance_km", precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "elevation_gain_m")
    private Integer elevationGainM;

    @Column(name = "elevation_loss_m")
    private Integer elevationLossM;

    @Column(name = "walking_hours_min", precision = 4, scale = 1)
    private BigDecimal walkingHoursMin;

    @Column(name = "walking_hours_max", precision = 4, scale = 1)
    private BigDecimal walkingHoursMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_difficulty", length = 20)
    private DifficultyLevel dayDifficulty;

    @Column(name = "suggested_start_time")
    private LocalTime suggestedStartTime;

    @Column(name = "suggested_end_time")
    private LocalTime suggestedEndTime;

    // JSONB: [{"type":"breakfast","location":"tại trại"}]
    @Column(name = "meals_included", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String mealsIncluded = "[]";

    @Column(name = "meal_notes", columnDefinition = "TEXT")
    private String mealNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overnight_waypoint_id")
    private TourWaypoint overnightWaypoint;

    @Column(name = "overnight_notes", columnDefinition = "TEXT")
    private String overnightNotes;

    @Column(name = "safety_notes", columnDefinition = "TEXT")
    private String safetyNotes;

    @Column(name = "guide_notes", columnDefinition = "TEXT")
    private String guideNotes;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Short sortOrder = 0;

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("visitOrder ASC")
    @Builder.Default
    private List<ItineraryWaypoint> waypointLinks = new ArrayList<>();
}
