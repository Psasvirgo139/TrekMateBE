package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.AccommodationType;
import com.trekmate.backend.model.enums.WaterSourceType;
import com.trekmate.backend.model.enums.WaypointType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TourWaypointResponse(
        UUID id,
        String name,
        String slug,
        Short sequenceOrder,
        WaypointType waypointType,
        BigDecimal lat,
        BigDecimal lng,
        Integer elevationM,
        Short dayNumber,
        Boolean isDayEnd,
        String description,
        String notesForGuide,
        Boolean hasToilet,
        Boolean hasShelter,
        Boolean hasPhoneSignal,
        Boolean hasFirstAid,
        WaterSourceType waterSource,
        String waterNotes,
        AccommodationType accommodation,
        Short campsiteCapacity,
        Integer campsiteFeeVnd,
        String resupplyNotes,
        String emergencyPhone,
        String evacuationRouteNotes,
        String nearestHospital,
        BigDecimal hospitalDistanceKm,
        Boolean helicopterLanding,
        String imageUrl,
        String thumbnailUrl,
        Boolean isActive,
        LocalDateTime lastVerifiedAt,
        UUID verifiedBy
) {}
