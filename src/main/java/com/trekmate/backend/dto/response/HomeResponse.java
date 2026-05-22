package com.trekmate.backend.dto.response;

import java.util.List;

public record HomeResponse(
        StatsResponse stats,
        List<TourCardResponse> featuredTours,
        List<DepartureCardResponse> upcomingDepartures,
        List<GuideCardResponse> topGuides
) {}
