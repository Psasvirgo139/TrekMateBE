package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.DepartureStatus;
import java.time.LocalDate;
import java.util.UUID;

public record GuideDepartureResponse(
        UUID departureId,
        String tourTitle,
        LocalDate departureDate,
        LocalDate returnDate,
        DepartureStatus status,
        int participantCount
) {}
