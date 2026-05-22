package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.WarningLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeatherDayResponse(
        Short dayNumber,
        LocalDate forecastDate,
        String locationLabel,
        Integer elevationM,
        String weatherSummary,
        String weatherIcon,
        Short tempMinC,
        Short tempMaxC,
        Short feelsLikeMinC,
        Short feelsLikeMaxC,
        BigDecimal precipitationMm,
        Short precipitationProb,
        Short windSpeedKmh,
        Short windGustKmh,
        Short humidityPct,
        BigDecimal visibilityKm,
        String weatherWarning,
        WarningLevel warningLevel
) {}
