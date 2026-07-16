package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.*;
import com.trekmate.backend.dto.response.*;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.mapper.TourMapper;
import com.trekmate.backend.model.*;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.repository.*;
import com.trekmate.backend.service.TourService;
import com.trekmate.backend.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
    private final TourWaypointRepository waypointRepository;
    private final TourDailyItineraryRepository itineraryRepository;
    private final ItineraryWaypointRepository itineraryWaypointRepository;
    private final TourImageRepository imageRepository;
    private final TourDepartureRepository departureRepository;
    private final BookingRepository bookingRepository;
    private final TourMapper tourMapper;
    private final DepartureGuideRepository departureGuideRepository;
    private final DepartureWeatherDailyRepository weatherDailyRepository;

    // ────────────────────────────────────────────────────────────────────────────
    // Tour CRUD
    // ────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<TourDetailResponse> getAllTours(String search, DifficultyLevel difficulty, TourStatus status, Pageable pageable) {
        log.debug("Get all tours with search: {}, difficulty: {}, status: {}", search, difficulty, status);
        String searchParam = (search != null && !search.trim().isEmpty()) ? "%" + search.trim().toLowerCase() + "%" : null;
        Page<Tour> tours = tourRepository.findWithFilters(searchParam, difficulty, status, pageable);
        return tours.map(tourMapper::toTourDetailResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TourDetailResponse getTourByIdOrSlug(String idOrSlug) {
        log.debug("Get tour by ID or Slug: {}", idOrSlug);
        Tour tour = resolveTour(idOrSlug);
        loadTourDetailRelations(tour);
        return tourMapper.toTourDetailResponse(tour);
    }

    @Override
    @Transactional
    public TourDetailResponse createTour(TourRequest request) {
        log.debug("Creating tour: {}", request.title());
        
        String slug = StringUtils.isNullOrBlank(request.slug()) ? 
                StringUtils.slugify(request.title()) : StringUtils.slugify(request.slug());
        
        slug = generateUniqueSlug(slug, null);

        Tour tour = tourMapper.toTour(request);
        tour.setSlug(slug);

        if (tour.getStatus() == null) {
            tour.setStatus(TourStatus.DRAFT);
        }

        Tour saved = tourRepository.save(tour);
        return tourMapper.toTourDetailResponse(saved);
    }

    @Override
    @Transactional
    public TourDetailResponse updateTour(UUID id, TourRequest request) {
        log.debug("Updating tour with ID: {}", id);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        String targetSlug = StringUtils.isNullOrBlank(request.slug()) ? 
                StringUtils.slugify(request.title()) : StringUtils.slugify(request.slug());
        
        if (!targetSlug.equals(tour.getSlug())) {
            targetSlug = generateUniqueSlug(targetSlug, id);
            tour.setSlug(targetSlug);
        }

        tourMapper.updateTour(request, tour);
        Tour saved = tourRepository.save(tour);
        return tourMapper.toTourDetailResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTour(UUID id) {
        log.debug("Deleting tour with ID: {}", id);
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        long upcomingDepartures = departureRepository.countUpcomingByTourId(id);
        long bookings = bookingRepository.countByDepartureTourId(id);

        if (upcomingDepartures > 0 || bookings > 0) {
            log.info("Tour has active dependencies. Performing soft delete (ARCHIVED). Tour ID: {}", id);
            tour.setStatus(TourStatus.ARCHIVED);
            tourRepository.save(tour);
        } else {
            log.info("Tour has no active dependencies. Performing hard delete. Tour ID: {}", id);
            tourRepository.delete(tour);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Waypoints CRUD
    // ────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TourWaypointResponse addWaypoint(UUID tourId, TourWaypointRequest request) {
        log.debug("Adding waypoint to tour ID: {}", tourId);
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        boolean existsOrder = waypointRepository.findByTourIdOrderBySequenceOrderAsc(tourId).stream()
                .anyMatch(w -> w.getSequenceOrder().equals(request.sequenceOrder()));
        if (existsOrder) {
            throw new AppException(ErrorCode.DUPLICATE_WAYPOINT_ORDER);
        }

        TourWaypoint wp = tourMapper.toWaypoint(request, tour);
        wp.setSlug(StringUtils.slugify(request.name()));
        TourWaypoint saved = waypointRepository.save(wp);
        return tourMapper.toWaypointResponse(saved);
    }

    @Override
    @Transactional
    public TourWaypointResponse updateWaypoint(UUID tourId, UUID waypointId, TourWaypointRequest request) {
        log.debug("Updating waypoint ID: {} in tour ID: {}", waypointId, tourId);
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        TourWaypoint wp = waypointRepository.findById(waypointId)
                .orElseThrow(() -> new AppException(ErrorCode.WAYPOINT_NOT_FOUND));

        if (!wp.getTour().getId().equals(tourId)) {
            throw new AppException(ErrorCode.WAYPOINT_NOT_FOUND);
        }

        boolean existsOrder = waypointRepository.findByTourIdOrderBySequenceOrderAsc(tourId).stream()
                .anyMatch(w -> !w.getId().equals(waypointId) && w.getSequenceOrder().equals(request.sequenceOrder()));
        if (existsOrder) {
            throw new AppException(ErrorCode.DUPLICATE_WAYPOINT_ORDER);
        }

        tourMapper.updateWaypoint(request, wp);
        wp.setSlug(StringUtils.slugify(request.name()));
        TourWaypoint saved = waypointRepository.save(wp);
        return tourMapper.toWaypointResponse(saved);
    }

    @Override
    @Transactional
    public void deleteWaypoint(UUID tourId, UUID waypointId) {
        log.debug("Deleting waypoint ID: {} from tour ID: {}", waypointId, tourId);
        TourWaypoint wp = waypointRepository.findById(waypointId)
                .orElseThrow(() -> new AppException(ErrorCode.WAYPOINT_NOT_FOUND));

        if (!wp.getTour().getId().equals(tourId)) {
            throw new AppException(ErrorCode.WAYPOINT_NOT_FOUND);
        }

        waypointRepository.delete(wp);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Daily Itinerary CRUD
    // ────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TourDailyItineraryResponse addOrUpdateDailyItinerary(UUID tourId, TourDailyItineraryRequest request) {
        log.debug("Adding or updating daily itinerary day: {} for tour ID: {}", request.dayNumber(), tourId);
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        // Find existing day itinerary or create new
        TourDailyItinerary itin = itineraryRepository.findByTourIdAndDayNumber(tourId, request.dayNumber())
                .orElse(null);

        if (itin == null) {
            itin = tourMapper.toItinerary(request, tour);
        } else {
            tourMapper.updateItinerary(request, itin);
        }

        TourDailyItinerary savedItin = itineraryRepository.save(itin);

        // Update waypoint links (intermediate waypoint itinerary links)
        if (request.waypointLinks() != null) {
            // Remove old links
            List<ItineraryWaypoint> oldLinks = new ArrayList<>(savedItin.getWaypointLinks());
            savedItin.getWaypointLinks().clear();
            itineraryWaypointRepository.deleteAll(oldLinks);

            // Add new links
            for (ItineraryWaypointRequest linkReq : request.waypointLinks()) {
                TourWaypoint waypoint = waypointRepository.findById(linkReq.waypointId())
                        .orElseThrow(() -> new AppException(ErrorCode.WAYPOINT_NOT_FOUND));
                
                if (!waypoint.getTour().getId().equals(tourId)) {
                    throw new AppException(ErrorCode.WAYPOINT_NOT_FOUND, "Waypoint does not belong to this tour");
                }

                ItineraryWaypoint iw = ItineraryWaypoint.builder()
                        .itinerary(savedItin)
                        .waypoint(waypoint)
                        .visitOrder(linkReq.visitOrder())
                        .isMandatory(linkReq.isMandatory() == null || linkReq.isMandatory())
                        .visitNotes(linkReq.visitNotes())
                        .estimatedArrival(linkReq.estimatedArrival())
                        .build();
                savedItin.getWaypointLinks().add(iw);
            }
            savedItin = itineraryRepository.save(savedItin);
        }

        return tourMapper.toItineraryResponse(savedItin);
    }

    @Override
    @Transactional
    public void deleteDailyItinerary(UUID tourId, UUID itineraryId) {
        log.debug("Deleting daily itinerary ID: {} from tour ID: {}", itineraryId, tourId);
        TourDailyItinerary itin = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new AppException(ErrorCode.ITINERARY_NOT_FOUND));

        if (!itin.getTour().getId().equals(tourId)) {
            throw new AppException(ErrorCode.ITINERARY_NOT_FOUND);
        }

        itineraryRepository.delete(itin);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Images CRUD
    // ────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TourImageResponse addTourImage(UUID tourId, TourImageRequest request) {
        log.debug("Adding image to tour ID: {}", tourId);
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        TourImage img = TourImage.builder()
                .tour(tour)
                .imageUrl(request.imageUrl())
                .caption(request.caption())
                .altText(request.altText())
                .isCover(request.isCover() != null && request.isCover())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build();

        TourImage saved = imageRepository.save(img);
        return tourMapper.toImageResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTourImage(UUID tourId, Long imageId) {
        log.debug("Deleting image ID: {} from tour ID: {}", imageId, tourId);
        TourImage img = imageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tour image not found"));

        if (!img.getTour().getId().equals(tourId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tour image does not belong to this tour");
        }

        imageRepository.delete(img);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Tour Search/Listing (dev)
    // ────────────────────────────────────────────────────────────────────────────

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

        // Dùng query trực tiếp để tránh LazyInitializationException trên LAZY images
        List<String> coverUrls = imageRepository.findCoverUrlsByTourId(t.getId());
        String coverUrl = coverUrls.isEmpty()
                ? imageRepository.findFirstImageUrlByTourId(t.getId()).stream().findFirst().orElse(null)
                : coverUrls.get(0);

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
                t.getHighlights(),
                coverUrl
        );
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ────────────────────────────────────────────────────────────────────────────

    private String generateUniqueSlug(String baseSlug, UUID tourIdToExclude) {
        String slug = baseSlug;
        int count = 1;
        while (true) {
            Optional<Tour> tourOpt = tourRepository.findBySlug(slug);
            if (tourOpt.isEmpty() || (tourIdToExclude != null && tourOpt.get().getId().equals(tourIdToExclude))) {
                return slug;
            }
            slug = baseSlug + "-" + count;
            count++;
        }
    }

    private Tour resolveTour(String idOrSlug) {
        try {
            UUID id = UUID.fromString(idOrSlug);
            return tourRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            return tourRepository.findBySlug(idOrSlug)
                    .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        }
    }

    private void loadTourDetailRelations(Tour tour) {
        tour.setImages(imageRepository.findByTourIdOrderBySortOrderAsc(tour.getId()));
        tour.setWaypoints(waypointRepository.findByTourIdOrderBySequenceOrderAsc(tour.getId()));

        List<TourDailyItinerary> itineraries = itineraryRepository.findByTourIdOrderByDayNumberAsc(tour.getId());
        for (TourDailyItinerary itinerary : itineraries) {
            itinerary.setWaypointLinks(loadItineraryWaypointLinks(itinerary.getId()));
        }
        tour.setDailyItinerary(itineraries);
    }

    private List<ItineraryWaypoint> loadItineraryWaypointLinks(UUID itineraryId) {
        return new ArrayList<>(itineraryWaypointRepository.findByItineraryIdOrderByVisitOrderAsc(itineraryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartureCardResponse> getUpcomingDepartures(String idOrSlug) {
        Tour tour = resolveTour(idOrSlug);
        List<com.trekmate.backend.model.enums.DepartureStatus> statuses = 
                List.of(com.trekmate.backend.model.enums.DepartureStatus.OPEN, 
                        com.trekmate.backend.model.enums.DepartureStatus.SCHEDULED);

        List<TourDeparture> departures = departureRepository.findUpcomingDepartures(
                tour.getId(), statuses, LocalDate.now());

        return departures.stream()
                .map(this::toDepartureCard)
                .collect(Collectors.toList());
    }

    private DepartureCardResponse toDepartureCard(TourDeparture dep) {
        Tour tour = dep.getTour();

        List<String> guideNames = departureGuideRepository.findByDepartureId(dep.getId())
                .stream()
                .map(dg -> dg.getGuide().getDisplayName())
                .collect(Collectors.toList());

        List<WeatherDayResponse> weatherDaily = weatherDailyRepository
                .findByDepartureIdOrderByDayNumberAsc(dep.getId())
                .stream()
                .map(this::toWeatherDay)
                .collect(Collectors.toList());

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

    private int warningLevelToInt(com.trekmate.backend.model.enums.WarningLevel wl) {
        if (wl == null) return 0;
        return switch (wl) {
            case INFO -> 1;
            case CAUTION -> 2;
            case WARNING -> 3;
            case DANGER -> 4;
        };
    }
}
