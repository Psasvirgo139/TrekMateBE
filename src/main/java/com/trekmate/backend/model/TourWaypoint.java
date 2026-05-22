package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.AccommodationType;
import com.trekmate.backend.model.enums.WaypointType;
import com.trekmate.backend.model.enums.WaterSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tour_waypoints", indexes = {
        @Index(name = "idx_waypoints_tour", columnList = "tour_id, sequence_order"),
        @Index(name = "idx_waypoints_type", columnList = "tour_id, waypoint_type"),
        @Index(name = "idx_waypoints_day",  columnList = "tour_id, day_number")
},
       uniqueConstraints = @UniqueConstraint(
               name = "uq_waypoint_order",
               columnNames = {"tour_id", "sequence_order"}
       ))
@EntityListeners(AuditingEntityListener.class)
public class TourWaypoint extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", length = 200)
    private String slug;

    @Column(name = "sequence_order", nullable = false)
    private Short sequenceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "waypoint_type", nullable = false, length = 30)
    private WaypointType waypointType;

    @Column(name = "lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "elevation_m")
    private Integer elevationM;

    @Column(name = "day_number")
    private Short dayNumber;

    @Column(name = "is_day_end", nullable = false)
    @Builder.Default
    private Boolean isDayEnd = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes_for_guide", columnDefinition = "TEXT")
    private String notesForGuide;

    @Column(name = "has_toilet", nullable = false)
    @Builder.Default
    private Boolean hasToilet = false;

    @Column(name = "has_shelter", nullable = false)
    @Builder.Default
    private Boolean hasShelter = false;

    @Column(name = "has_phone_signal", nullable = false)
    @Builder.Default
    private Boolean hasPhoneSignal = false;

    @Column(name = "has_first_aid", nullable = false)
    @Builder.Default
    private Boolean hasFirstAid = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_source", nullable = false, length = 20)
    @Builder.Default
    private WaterSourceType waterSource = WaterSourceType.NONE;

    @Column(name = "water_notes", columnDefinition = "TEXT")
    private String waterNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "accommodation", length = 20)
    private AccommodationType accommodation;

    @Column(name = "campsite_capacity")
    private Short campsiteCapacity;

    @Column(name = "campsite_fee_vnd")
    private Integer campsiteFeeVnd;

    @Column(name = "resupply_notes", columnDefinition = "TEXT")
    private String resupplyNotes;

    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    @Column(name = "evacuation_route_notes", columnDefinition = "TEXT")
    private String evacuationRouteNotes;

    @Column(name = "nearest_hospital", length = 255)
    private String nearestHospital;

    @Column(name = "hospital_distance_km", precision = 6, scale = 1)
    private BigDecimal hospitalDistanceKm;

    @Column(name = "helicopter_landing", nullable = false)
    @Builder.Default
    private Boolean helicopterLanding = false;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_verified_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @OneToMany(mappedBy = "waypoint", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItineraryWaypoint> itineraryLinks = new ArrayList<>();
}

