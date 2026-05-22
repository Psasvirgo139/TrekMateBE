package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.DepartureStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "tour_departures", indexes = {
        @Index(name = "idx_departures_tour",   columnList = "tour_id"),
        @Index(name = "idx_departures_date",   columnList = "departure_date"),
        @Index(name = "idx_departures_status", columnList = "status")
},
       uniqueConstraints = @UniqueConstraint(
               name = "uq_tour_departure",
               columnNames = {"tour_id", "departure_date"}
       ))
@EntityListeners(AuditingEntityListener.class)
public class TourDeparture extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    // ── Thời gian ──────────────────────────────────────────────────────────────

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "cutoff_date")
    private LocalDate cutoffDate;

    @Column(name = "actual_start_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime actualStartAt;

    @Column(name = "actual_end_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime actualEndAt;

    // ── Giá & Nhóm ─────────────────────────────────────────────────────────────

    @Column(name = "price_per_person", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerPerson;

    @Column(name = "max_group_size", nullable = false)
    private Short maxGroupSize;

    @Column(name = "min_group_size", nullable = false)
    @Builder.Default
    private Short minGroupSize = 2;

    @Column(name = "booked_slots", nullable = false)
    @Builder.Default
    private Short bookedSlots = 0;

    @Column(name = "allow_join_tour", nullable = false)
    @Builder.Default
    private Boolean allowJoinTour = true;

    // ── Điểm tập kết ───────────────────────────────────────────────────────────

    @Column(name = "meeting_point", columnDefinition = "TEXT")
    private String meetingPoint;

    @Column(name = "meeting_lat", precision = 10, scale = 7)
    private BigDecimal meetingLat;

    @Column(name = "meeting_lng", precision = 10, scale = 7)
    private BigDecimal meetingLng;

    // ── Thời tiết tổng quan (overview cho listing) ──────────────────────────────
    // Chi tiết từng ngày → bảng DepartureWeatherDaily

    @Column(name = "weather_summary", length = 100)
    private String weatherSummary;

    @Column(name = "weather_icon", length = 50)
    private String weatherIcon;

    @Column(name = "temp_min_c")
    private Short tempMinC;

    @Column(name = "temp_max_c")
    private Short tempMaxC;

    @Column(name = "precipitation_mm", precision = 5, scale = 1)
    private BigDecimal precipitationMm;

    @Column(name = "wind_speed_kmh")
    private Short windSpeedKmh;

    @Column(name = "weather_updated_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime weatherUpdatedAt;

    @Column(name = "weather_warning", columnDefinition = "TEXT")
    private String weatherWarning;

    // ── Log vận hành ───────────────────────────────────────────────────────────

    @Column(name = "actual_participants")
    private Short actualParticipants;

    // JSONB: [{"time":"...","type":"injury","note":"...","handled_by":"..."}]
    @Column(name = "incident_log", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String incidentLog = "[]";

    // JSONB: [{"reason":"weather","original":"...","actual":"..."}]
    @Column(name = "route_deviations", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String routeDeviations = "[]";

    @Column(name = "debrief_notes", columnDefinition = "TEXT")
    private String debriefNotes;

    // ── Trạng thái ─────────────────────────────────────────────────────────────

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DepartureStatus status = DepartureStatus.SCHEDULED;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "created_by")
    private UUID createdBy;

    // ── Relationships ───────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "departure", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<DepartureGuide> guideAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "departure", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @Builder.Default
    private List<DepartureWeatherDaily> weatherDailyList = new ArrayList<>();

    @OneToMany(mappedBy = "departure", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();
}

