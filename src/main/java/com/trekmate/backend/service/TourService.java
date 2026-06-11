package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.*;
import com.trekmate.backend.dto.response.*;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TourService {

    // Tour CRUD
    Page<TourDetailResponse> getAllTours(String search, DifficultyLevel difficulty, TourStatus status, Pageable pageable);
    
    TourDetailResponse getTourByIdOrSlug(String idOrSlug);
    
    TourDetailResponse createTour(TourRequest request);
    
    TourDetailResponse updateTour(UUID id, TourRequest request);
    
    void deleteTour(UUID id);

    // Tour Waypoints CRUD
    TourWaypointResponse addWaypoint(UUID tourId, TourWaypointRequest request);
    
    TourWaypointResponse updateWaypoint(UUID tourId, UUID waypointId, TourWaypointRequest request);
    
    void deleteWaypoint(UUID tourId, UUID waypointId);

    // Tour Daily Itinerary CRUD
    TourDailyItineraryResponse addOrUpdateDailyItinerary(UUID tourId, TourDailyItineraryRequest request);
    
    void deleteDailyItinerary(UUID tourId, UUID itineraryId);

    // Tour Images CRUD
    TourImageResponse addTourImage(UUID tourId, TourImageRequest request);
    
    void deleteTourImage(UUID tourId, Long imageId);

    // Tour Search/Listing (dev)
    Page<TourCardResponse> getTours(
            String search,
            DifficultyLevel difficulty,
            TourStatus status,
            Short minDuration,
            Short maxDuration,
            Pageable pageable
    );
}
