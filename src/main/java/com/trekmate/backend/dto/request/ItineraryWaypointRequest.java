package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.UUID;

public record ItineraryWaypointRequest(
        @NotNull(message = "Waypoint ID is required")
        UUID waypointId,

        @NotNull(message = "Visit order is required")
        Short visitOrder,

        Boolean isMandatory,
        String visitNotes,
        LocalTime estimatedArrival
) {}
