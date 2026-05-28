package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.response.*;
import com.trekmate.backend.model.*;
import com.trekmate.backend.model.enums.DepartureStatus;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.model.enums.WarningLevel;
import com.trekmate.backend.repository.*;
import com.trekmate.backend.service.HomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private static final int MAX_FEATURED_TOURS      = 6;
    private static final int MAX_UPCOMING_DEPARTURES = 6;
    private static final int MAX_TOP_GUIDES          = 6;

    private final TourRepository                  tourRepository;
    private final TourDepartureRepository         departureRepository;
    private final DepartureGuideRepository        departureGuideRepository;
    private final DepartureWeatherDailyRepository weatherDailyRepository;
    private final GuideRepository                 guideRepository;
    private final BookingRepository               bookingRepository;

    // ────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public HomeResponse getHomeData() {
        return new HomeResponse(
                buildStats(),
                buildFeaturedTours(),
                buildUpcomingDepartures(),
                buildTopGuides()
        );
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Stats
    // ────────────────────────────────────────────────────────────────────────────

    private StatsResponse buildStats() {
        return new StatsResponse(
                tourRepository.findByStatus(TourStatus.ACTIVE,
                        PageRequest.of(0, 1)).getTotalElements(),
                guideRepository.count(),
                departureRepository.countUpcoming(),
                bookingRepository.countCompleted()
        );
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Featured Tours
    // ────────────────────────────────────────────────────────────────────────────

    private List<TourCardResponse> buildFeaturedTours() {
        Sort byRating = Sort.by(Sort.Direction.DESC, "avgRating");
        return tourRepository
                .findByStatus(TourStatus.ACTIVE, PageRequest.of(0, MAX_FEATURED_TOURS, byRating))
                .getContent()
                .stream()
                .map(this::toTourCard)
                .collect(Collectors.toList());
    }

    private TourCardResponse toTourCard(Tour t) {
        BigDecimal priceFrom = departureRepository.findMinPriceByTourId(t.getId()).orElse(null);
        long upcoming        = departureRepository.countUpcomingByTourId(t.getId());

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

    // ────────────────────────────────────────────────────────────────────────────
    // Upcoming Departures
    // ────────────────────────────────────────────────────────────────────────────

    private List<DepartureCardResponse> buildUpcomingDepartures() {
        Sort byDate = Sort.by(Sort.Direction.ASC, "departureDate");
        return departureRepository
                .findByStatus(DepartureStatus.OPEN, PageRequest.of(0, MAX_UPCOMING_DEPARTURES, byDate))
                .getContent()
                .stream()
                .map(this::toDepartureCard)
                .collect(Collectors.toList());
    }

    private DepartureCardResponse toDepartureCard(TourDeparture dep) {
        Tour tour = dep.getTour();

        // Tên HDV dẫn chuyến
        List<String> guideNames = departureGuideRepository.findByDepartureId(dep.getId())
                .stream()
                .map(dg -> dg.getGuide().getDisplayName())
                .collect(Collectors.toList());

        // Thời tiết từng ngày
        List<WeatherDayResponse> weatherDaily = weatherDailyRepository
                .findByDepartureIdOrderByDayNumberAsc(dep.getId())
                .stream()
                .map(this::toWeatherDay)
                .collect(Collectors.toList());

        // Mức cảnh báo cao nhất trong cả chuyến
        int maxWarnLevel = weatherDaily.stream()
                .map(w -> warningLevelToInt(w.warningLevel()))
                .max(Integer::compareTo)
                .orElse(0);

        short available = (short) (dep.getMaxGroupSize() - dep.getBookedSlots());

        return new DepartureCardResponse(
                dep.getId(),
                tour.getId(),
                tour.getTitle(),
                tour.getSlug(),
                tour.getDifficulty(),
                tour.getDurationDays(),
                dep.getDepartureDate(),
                dep.getReturnDate(),
                dep.getCutoffDate(),
                dep.getPricePerPerson(),
                dep.getMaxGroupSize(),
                dep.getBookedSlots(),
                available,
                dep.getAllowJoinTour(),
                dep.getMeetingPoint(),
                dep.getWeatherSummary(),
                dep.getWeatherIcon(),
                dep.getTempMinC(),
                dep.getTempMaxC(),
                dep.getWeatherWarning(),
                maxWarnLevel,
                dep.getStatus(),
                guideNames,
                weatherDaily
        );
    }

    private WeatherDayResponse toWeatherDay(DepartureWeatherDaily w) {
        return new WeatherDayResponse(
                w.getDayNumber(),
                w.getForecastDate(),
                w.getLocationLabel(),
                w.getElevationM(),
                w.getWeatherSummary(),
                w.getWeatherIcon(),
                w.getTempMinC(),
                w.getTempMaxC(),
                w.getFeelsLikeMinC(),
                w.getFeelsLikeMaxC(),
                w.getPrecipitationMm(),
                w.getPrecipitationProb(),
                w.getWindSpeedKmh(),
                w.getWindGustKmh(),
                w.getHumidityPct(),
                w.getVisibilityKm(),
                w.getWeatherWarning(),
                w.getWarningLevel()
        );
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Top Guides
    // ────────────────────────────────────────────────────────────────────────────

    private List<GuideCardResponse> buildTopGuides() {
        return guideRepository
                .findAllByOrderByAvgRatingDesc(PageRequest.of(0, MAX_TOP_GUIDES))
                .getContent()
                .stream()
                .map(this::toGuideCard)
                .collect(Collectors.toList());
    }

    private GuideCardResponse toGuideCard(Guide g) {
        long upcoming = guideRepository.countUpcomingToursByGuideId(g.getId());

        return new GuideCardResponse(
                g.getId(),
                g.getDisplayName(),
                g.getAvatarUrl(),
                g.getHomeProvince(),
                g.getExperienceYears(),
                g.getAvgRating(),
                g.getTotalReviews(),
                g.getTotalToursLed(),
                g.getIsAvailable(),
                g.getLanguages(),
                g.getSpecializations(),
                upcoming
        );
    }


    private int warningLevelToInt(WarningLevel level) {
        if (level == null) return 0;
        return switch (level) {
            case INFO    -> 1;
            case CAUTION -> 2;
            case WARNING -> 3;
            case DANGER  -> 4;
        };
    }
}
