package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.WarningLevel;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "departure_weather_daily", indexes = {
        @Index(name = "idx_weather_daily_departure",  columnList = "departure_id, day_number"),
        @Index(name = "idx_weather_daily_date",       columnList = "forecast_date"),
        @Index(name = "idx_weather_daily_itinerary",  columnList = "itinerary_id")
},
       uniqueConstraints = @UniqueConstraint(
               name = "uq_weather_daily",
               columnNames = {"departure_id", "day_number"}
       ))
@EntityListeners(AuditingEntityListener.class)
public class DepartureWeatherDaily extends LongBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private TourDeparture departure;

    @Column(name = "day_number", nullable = false)
    private Short dayNumber;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    private TourDailyItinerary itinerary;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    @Column(name = "checkpoint_lat", precision = 10, scale = 7)
    private BigDecimal checkpointLat;

    @Column(name = "checkpoint_lng", precision = 10, scale = 7)
    private BigDecimal checkpointLng;

    @Column(name = "elevation_m")
    private Integer elevationM;

    @Column(name = "weather_summary", length = 150)
    private String weatherSummary;

    @Column(name = "weather_icon", length = 50)
    private String weatherIcon;

    @Column(name = "temp_min_c")
    private Short tempMinC;

    @Column(name = "temp_max_c")
    private Short tempMaxC;

    @Column(name = "feels_like_min_c")
    private Short feelsLikeMinC;

    @Column(name = "feels_like_max_c")
    private Short feelsLikeMaxC;

    @Column(name = "precipitation_mm", precision = 5, scale = 1)
    private BigDecimal precipitationMm;

    @Column(name = "precipitation_prob")
    private Short precipitationProb;

    @Column(name = "wind_speed_kmh")
    private Short windSpeedKmh;

    @Column(name = "wind_direction", length = 10)
    private String windDirection;

    @Column(name = "wind_gust_kmh")
    private Short windGustKmh;

    @Column(name = "humidity_pct")
    private Short humidityPct;

    @Column(name = "visibility_km", precision = 5, scale = 1)
    private BigDecimal visibilityKm;

    @Column(name = "uv_index")
    private Short uvIndex;

    @Column(name = "cloud_cover_pct")
    private Short cloudCoverPct;

    @Column(name = "weather_warning", columnDefinition = "TEXT")
    private String weatherWarning;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_level", length = 20)
    private WarningLevel warningLevel;

    @Column(name = "data_source", nullable = false, length = 50)
    @Builder.Default
    private String dataSource = "manual";

    @Column(name = "forecast_updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    @Builder.Default
    private LocalDateTime forecastUpdatedAt = LocalDateTime.now();

    @Column(name = "is_actual", nullable = false)
    @Builder.Default
    private Boolean isActual = false;
}