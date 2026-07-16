package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.BulkDepartureRequest;
import com.trekmate.backend.dto.request.TourDepartureRequest;
import com.trekmate.backend.dto.response.DepartureCardResponse;
import com.trekmate.backend.dto.response.WeatherDayResponse;
import com.trekmate.backend.dto.response.AvailableGuideResponse;
import com.trekmate.backend.dto.response.GuideScheduleResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.DepartureWeatherDaily;
import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.model.enums.DepartureStatus;
import com.trekmate.backend.model.enums.WarningLevel;
import com.trekmate.backend.repository.DepartureGuideRepository;
import com.trekmate.backend.repository.DepartureWeatherDailyRepository;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.repository.TourRepository;
import com.trekmate.backend.repository.GuideRepository;
import com.trekmate.backend.model.Guide;
import com.trekmate.backend.model.DepartureGuide;
import com.trekmate.backend.service.TourDepartureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourDepartureServiceImpl implements TourDepartureService {

    private final TourRepository tourRepository;
    private final TourDepartureRepository departureRepository;
    private final DepartureGuideRepository departureGuideRepository;
    private final DepartureWeatherDailyRepository weatherDailyRepository;
    private final GuideRepository guideRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DepartureCardResponse> getTourDepartures(UUID tourId, Pageable pageable) {
        log.info("Fetching departures for tour ID: {}", tourId);
        if (!tourRepository.existsById(tourId)) {
            throw new AppException(ErrorCode.TOUR_NOT_FOUND);
        }
        Page<TourDeparture> departures = departureRepository.findByTourId(tourId, pageable);
        return departures.map(this::toDepartureCard);
    }

    @Override
    @Transactional
    public DepartureCardResponse createDeparture(UUID tourId, TourDepartureRequest request) {
        log.info("Creating single departure for tour ID: {} on date: {}", tourId, request.departureDate());
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        // Check duplicate departure date
        if (departureRepository.findByTourIdAndDepartureDate(tourId, request.departureDate()).isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "A departure on this date already exists for this tour");
        }

        short durationDays = tour.getDurationDays() != null ? tour.getDurationDays() : 1;
        LocalDate departureDate = request.departureDate();
        LocalDate returnDate = request.returnDate() != null ? request.returnDate() : departureDate.plusDays(durationDays - 1);
        LocalDate cutoffDate = request.cutoffDate() != null ? request.cutoffDate() : departureDate.minusDays(1);

        TourDeparture departure = TourDeparture.builder()
                .tour(tour)
                .departureDate(departureDate)
                .returnDate(returnDate)
                .cutoffDate(cutoffDate)
                .pricePerPerson(request.pricePerPerson())
                .maxGroupSize(request.maxGroupSize())
                .minGroupSize(request.minGroupSize() != null ? request.minGroupSize() : 2)
                .allowJoinTour(request.allowJoinTour() != null ? request.allowJoinTour() : true)
                .meetingPoint(request.meetingPoint())
                .meetingLat(request.meetingLat())
                .meetingLng(request.meetingLng())
                .notes(request.notes())
                .status(DepartureStatus.SCHEDULED)
                .bookedSlots((short) 0)
                .build();

        TourDeparture saved = departureRepository.saveAndFlush(departure);
        validateAndAssignGuides(saved, request.guideIds(), null);
        return toDepartureCard(saved);
    }

    @Override
    @Transactional
    public List<DepartureCardResponse> generateBulkDepartures(UUID tourId, BulkDepartureRequest request) {
        log.info("Generating bulk departures for tour ID: {} from {} to {}", tourId, request.startDate(), request.endDate());
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        if (request.startDate().isAfter(request.endDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Start date cannot be after end date");
        }

        short durationDays = tour.getDurationDays() != null ? tour.getDurationDays() : 1;
        List<TourDeparture> toSave = new ArrayList<>();

        LocalDate currentDate = request.startDate();
        while (!currentDate.isAfter(request.endDate())) {
            if (request.daysOfWeek().contains(currentDate.getDayOfWeek())) {
                // Skip if duplicate exists
                if (departureRepository.findByTourIdAndDepartureDate(tourId, currentDate).isEmpty()) {
                    LocalDate returnDate = currentDate.plusDays(durationDays - 1);
                    LocalDate cutoffDate = currentDate.minusDays(1);

                    TourDeparture dep = TourDeparture.builder()
                            .tour(tour)
                            .departureDate(currentDate)
                            .returnDate(returnDate)
                            .cutoffDate(cutoffDate)
                            .pricePerPerson(request.pricePerPerson())
                            .maxGroupSize(request.maxGroupSize())
                            .minGroupSize(request.minGroupSize() != null ? request.minGroupSize() : 2)
                            .allowJoinTour(request.allowJoinTour() != null ? request.allowJoinTour() : true)
                            .meetingPoint(request.meetingPoint())
                            .meetingLat(request.meetingLat())
                            .meetingLng(request.meetingLng())
                            .notes(request.notes())
                            .status(DepartureStatus.SCHEDULED)
                            .bookedSlots((short) 0)
                            .build();

                    toSave.add(dep);
                } else {
                    log.warn("Departure date {} already exists for tour ID {}, skipping.", currentDate, tourId);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        List<TourDeparture> saved = departureRepository.saveAll(toSave);
        departureRepository.flush();

        if (request.guideIds() != null && !request.guideIds().isEmpty()) {
            for (TourDeparture dep : saved) {
                validateAndAssignGuides(dep, request.guideIds(), null);
            }
        }

        return saved.stream().map(this::toDepartureCard).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DepartureCardResponse updateDeparture(UUID tourId, UUID departureId, TourDepartureRequest request) {
        log.info("Updating departure ID: {} for tour ID: {}", departureId, tourId);
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found"));

        if (!departure.getTour().getId().equals(tourId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure does not belong to the specified tour");
        }

        // If departureDate changed, verify no duplicate
        if (!departure.getDepartureDate().equals(request.departureDate())) {
            if (departureRepository.findByTourIdAndDepartureDate(tourId, request.departureDate()).isPresent()) {
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "A departure on this date already exists for this tour");
            }
            departure.setDepartureDate(request.departureDate());
        }

        Tour tour = departure.getTour();
        short durationDays = tour.getDurationDays() != null ? tour.getDurationDays() : 1;

        departure.setReturnDate(request.returnDate() != null ? request.returnDate() : request.departureDate().plusDays(durationDays - 1));
        departure.setCutoffDate(request.cutoffDate() != null ? request.cutoffDate() : request.departureDate().minusDays(1));
        departure.setPricePerPerson(request.pricePerPerson());
        departure.setMaxGroupSize(request.maxGroupSize());
        if (request.minGroupSize() != null) {
            departure.setMinGroupSize(request.minGroupSize());
        }
        if (request.allowJoinTour() != null) {
            departure.setAllowJoinTour(request.allowJoinTour());
        }
        if (request.meetingPoint() != null) {
            departure.setMeetingPoint(request.meetingPoint());
        }
        departure.setMeetingLat(request.meetingLat());
        departure.setMeetingLng(request.meetingLng());
        departure.setNotes(request.notes());

        // Clear existing assignments
        List<DepartureGuide> currentAssignments = departureGuideRepository.findByDepartureId(departureId);
        departureGuideRepository.deleteAll(currentAssignments);
        departureGuideRepository.flush();

        TourDeparture saved = departureRepository.saveAndFlush(departure);
        validateAndAssignGuides(saved, request.guideIds(), departureId);
        return toDepartureCard(saved);
    }

    @Override
    @Transactional
    public void deleteDeparture(UUID tourId, UUID departureId) {
        log.info("Deleting departure ID: {} for tour ID: {}", departureId, tourId);
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found"));

        if (!departure.getTour().getId().equals(tourId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure does not belong to the specified tour");
        }

        // If it already has bookings, we soft delete/cancel it or throw error?
        if (departure.getBookedSlots() > 0) {
            log.info("Departure has active bookings. Marking as CANCELLED. ID: {}", departureId);
            departure.setStatus(DepartureStatus.CANCELLED);
            departureRepository.save(departure);
        } else {
            log.info("Departure has no bookings. Performing hard delete. ID: {}", departureId);
            departureRepository.delete(departure);
        }
    }

    // ── Helper Mapper ──────────────────────────────────────────────────────────

    private DepartureCardResponse toDepartureCard(TourDeparture dep) {
        Tour tour = dep.getTour();

        List<String> guideNames = departureGuideRepository.findByDepartureId(dep.getId())
                .stream()
                .filter(dg -> dg.getGuide() != null)
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

    private int warningLevelToInt(WarningLevel level) {
        if (level == null) return 0;
        return switch (level) {
            case INFO    -> 1;
            case CAUTION -> 2;
            case WARNING -> 3;
            case DANGER  -> 4;
        };
    }

    private void validateAndAssignGuides(TourDeparture departure, List<UUID> guideIds, UUID excludeDepartureId) {
        if (guideIds == null || guideIds.isEmpty()) {
            return;
        }
        for (UUID guideId : guideIds) {
            Guide g = guideRepository.findById(guideId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Guide not found with ID " + guideId));

            if (g.getIsAvailable() == null || !g.getIsAvailable() || g.getUser() == null || g.getUser().getIsActive() == null || !g.getUser().getIsActive()) {
                throw new AppException(ErrorCode.TOUR_NOT_AVAILABLE, "Guide " + g.getDisplayName() + " is currently inactive or not available");
            }

            List<DepartureGuide> conflicts = departureGuideRepository.findConflicts(
                    guideId, excludeDepartureId, departure.getDepartureDate(), departure.getReturnDate());
            if (!conflicts.isEmpty()) {
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "Guide " + g.getDisplayName() + " has conflicting schedules during this period");
            }

            DepartureGuide assignment = DepartureGuide.builder()
                    .id(new com.trekmate.backend.model.embeddable.DepartureGuideId(departure.getId(), g.getId()))
                    .departure(departure)
                    .guide(g)
                    .role(com.trekmate.backend.model.enums.GuideRoleInTour.LEAD)
                    .confirmedAt(java.time.LocalDateTime.now())
                    .build();
            departureGuideRepository.saveAndFlush(assignment);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableGuideResponse> getAvailableGuides(LocalDate startDate, LocalDate endDate, UUID excludeDepartureId) {
        log.info("Fetching available guides from {} to {} excluding departure {}", startDate, endDate, excludeDepartureId);
        List<Guide> guides = guideRepository.findAvailableGuides(startDate, endDate, excludeDepartureId);
        return guides.stream()
                .map(g -> new AvailableGuideResponse(
                        g.getId(),
                        g.getDisplayName(),
                        g.getUser() != null ? g.getUser().getPhone() : null,
                        g.getExperienceYears(),
                        g.getAvgRating(),
                        g.getAvatarUrl()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideScheduleResponse> getGuideSchedules(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching guide schedules from {} to {}", startDate, endDate);
        List<DepartureGuide> assignments = departureGuideRepository.findAssignmentsInPeriod(startDate, endDate);
        return assignments.stream()
                .map(dg -> new GuideScheduleResponse(
                        dg.getGuide().getId(),
                        dg.getGuide().getDisplayName(),
                        dg.getGuide().getUser() != null ? dg.getGuide().getUser().getPhone() : null,
                        dg.getDeparture().getId(),
                        dg.getDeparture().getTour().getTitle(),
                        dg.getDeparture().getDepartureDate(),
                        dg.getDeparture().getReturnDate(),
                        dg.getRole() != null ? dg.getRole().name() : "LEAD",
                        dg.getDeparture().getStatus() != null ? dg.getDeparture().getStatus().name() : "SCHEDULED"
                ))
                .collect(Collectors.toList());
    }
}
