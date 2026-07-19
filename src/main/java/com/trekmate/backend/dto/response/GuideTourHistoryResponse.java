package com.trekmate.backend.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record GuideTourHistoryResponse(
        UUID departureId,
        String tourTitle,
        LocalDate departureDate,
        Double rating,
        String status
) {}
