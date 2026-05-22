package com.trekmate.backend.dto.response;

public record StatsResponse(
        long totalActiveTours,
        long totalGuides,
        long totalUpcomingDepartures,
        long totalCompletedBookings
) {}
