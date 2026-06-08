package com.trekmate.backend.dto.request;

import com.trekmate.backend.model.enums.AccommodationType;
import com.trekmate.backend.model.enums.WaterSourceType;
import com.trekmate.backend.model.enums.WaypointType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TourWaypointRequest(
        @NotBlank(message = "Name cannot be blank")
        @Size(max = 200, message = "Name cannot exceed 200 characters")
        String name,

        @NotNull(message = "Sequence order is required")
        Short sequenceOrder,

        @NotNull(message = "Waypoint type is required")
        WaypointType waypointType,

        @NotNull(message = "Latitude is required")
        BigDecimal lat,

        @NotNull(message = "Longitude is required")
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
        Boolean isActive
) {}
