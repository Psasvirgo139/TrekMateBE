package com.trekmate.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Bảng nối tour_daily_itinerary <-> tour_waypoints.
 * PK là UUID để cho phép cùng một waypoint xuất hiện nhiều lần
 * trong một ngày (ví dụ: qua điểm thoát hiểm cả lúc lên lẫn xuống).
 * UNIQUE constraint trên (itinerary_id, visit_order) đảm bảo không có
 * hai điểm có cùng thứ tự trong một ngày.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "itinerary_waypoints",
       indexes = @Index(name = "idx_itin_waypoints_wp", columnList = "waypoint_id"),
       uniqueConstraints = @UniqueConstraint(
               name = "uq_itinerary_visit_order",
               columnNames = {"itinerary_id", "visit_order"}
       ))
public class ItineraryWaypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private TourDailyItinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waypoint_id", nullable = false)
    private TourWaypoint waypoint;

    @Column(name = "visit_order", nullable = false)
    private Short visitOrder;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = true;

    @Column(name = "visit_notes", columnDefinition = "TEXT")
    private String visitNotes;

    @Column(name = "estimated_arrival")
    private LocalTime estimatedArrival;
}
