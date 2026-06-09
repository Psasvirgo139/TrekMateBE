package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.response.TourCardResponse;
import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.repository.TourRepository;
import com.trekmate.backend.service.TourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
    private final TourDepartureRepository departureRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TourCardResponse> getTours(
            String search,
            DifficultyLevel difficulty,
            TourStatus status,
            Short minDuration,
            Short maxDuration,
            Pageable pageable
    ) {
        log.info("Fetching tours with search='{}', difficulty='{}', status='{}', duration={}-{} days",
                search, difficulty, status, minDuration, maxDuration);

        // Mặc định lọc ACTIVE nếu trạng thái không được cung cấp (cho client hiển thị các tour đang hoạt động)
        TourStatus activeStatus = (status != null) ? status : TourStatus.ACTIVE;

        Page<Tour> toursPage = tourRepository.findToursWithFilters(
                search, difficulty, activeStatus, minDuration, maxDuration, pageable
        );

        return toursPage.map(this::toTourCard);
    }

    private TourCardResponse toTourCard(Tour t) {
        BigDecimal priceFrom = departureRepository.findMinPriceByTourId(t.getId()).orElse(null);
        long upcoming = departureRepository.countUpcomingByTourId(t.getId());

        return new TourCardResponse(
                t.getId(),
                t.getTitle(),
                t.getSlug(),
                t.getDifficulty(),
                t.getDurationDays(),
                t.getDurationNights(),
                t.getDistanceKm(),
                t.getMaxElevationM(),
                t.getStartLocation(),
                t.getEndLocation(),
                t.getAvgRating(),
                t.getTotalReviews(),
                t.getTotalDepartures(),
                t.getStatus(),
                priceFrom,
                upcoming,
                t.getHighlights()
        );
    }
}
