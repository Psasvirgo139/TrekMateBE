package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.BulkDepartureRequest;
import com.trekmate.backend.dto.request.TourDepartureRequest;
import com.trekmate.backend.dto.response.DepartureCardResponse;
import com.trekmate.backend.dto.response.AvailableGuideResponse;
import com.trekmate.backend.dto.response.GuideScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TourDepartureService {
    Page<DepartureCardResponse> getTourDepartures(UUID tourId, Pageable pageable);
    
    DepartureCardResponse createDeparture(UUID tourId, TourDepartureRequest request);
    
    List<DepartureCardResponse> generateBulkDepartures(UUID tourId, BulkDepartureRequest request);
    
    DepartureCardResponse updateDeparture(UUID tourId, UUID departureId, TourDepartureRequest request);
    
    void deleteDeparture(UUID tourId, UUID departureId);

    List<AvailableGuideResponse> getAvailableGuides(LocalDate startDate, LocalDate endDate, UUID excludeDepartureId);

    List<GuideScheduleResponse> getGuideSchedules(LocalDate startDate, LocalDate endDate);
}
