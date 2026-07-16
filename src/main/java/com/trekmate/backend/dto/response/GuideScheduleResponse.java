package com.trekmate.backend.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record GuideScheduleResponse(
    UUID guideId,
    String guideName,
    String phone,
    UUID departureId,
    String tourTitle,
    LocalDate startDate,
    LocalDate endDate,
    String role,
    String status
) {}
