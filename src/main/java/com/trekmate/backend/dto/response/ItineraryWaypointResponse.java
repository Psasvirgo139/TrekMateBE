package com.trekmate.backend.dto.response;

import java.time.LocalTime;
import java.util.UUID;

public record ItineraryWaypointResponse(
        Long id,
        UUID waypointId,
        String waypointName,
        Short visitOrder,
        Boolean isMandatory,
        String visitNotes,
        LocalTime estimatedArrival
) {}
