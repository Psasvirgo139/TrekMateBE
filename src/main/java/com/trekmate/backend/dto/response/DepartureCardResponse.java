package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.DepartureStatus;
import com.trekmate.backend.model.enums.DifficultyLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DepartureCardResponse(
        UUID departureId,
        UUID tourId,
        String tourTitle,
        String tourSlug,
        DifficultyLevel tourDifficulty,
        Short tourDurationDays,

        LocalDate departureDate,
        LocalDate returnDate,
        LocalDate cutoffDate,

        BigDecimal pricePerPerson,
        Short maxGroupSize,
        Short bookedSlots,
        Short availableSlots,
        Boolean allowJoinTour,

        String meetingPoint,

        // Thời tiết tổng quan (hiển thị trên card)
        String weatherSummary,
        String weatherIcon,
        Short tempMinC,
        Short tempMaxC,
        String weatherWarning,
        // Cấp cảnh báo nguy hiểm nhất trong toàn chuyến (0=none .. 4=danger)
        int maxWarningLevel,

        DepartureStatus status,

        // Danh sách tên HDV dẫn chuyến
        List<String> guideNames,
        // Dự báo thời tiết từng ngày
        List<WeatherDayResponse> weatherDaily
) {}
