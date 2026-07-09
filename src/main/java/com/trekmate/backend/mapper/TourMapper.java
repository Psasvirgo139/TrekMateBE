package com.trekmate.backend.mapper;

import com.trekmate.backend.dto.request.*;
import com.trekmate.backend.dto.response.*;
import com.trekmate.backend.model.*;
import com.trekmate.backend.model.enums.TourAttributeType;
import com.trekmate.backend.repository.TourWaypointRepository;
import com.trekmate.backend.repository.TourAttributeRepository;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TourMapper {

    private final TourWaypointRepository waypointRepository;
    private final TourAttributeRepository tourAttributeRepository;
    private final TourDepartureRepository departureRepository;
    private final BookingRepository bookingRepository;

    public TourDetailResponse toTourDetailResponse(Tour tour) {
        if (tour == null) return null;

        List<TourImageResponse> images = tour.getImages() != null ?
                tour.getImages().stream().map(this::toImageResponse).collect(Collectors.toList()) : new ArrayList<>();

        List<TourWaypointResponse> waypoints = tour.getWaypoints() != null ?
                tour.getWaypoints().stream().map(this::toWaypointResponse).collect(Collectors.toList()) : new ArrayList<>();

        List<TourDailyItineraryResponse> dailyItinerary = tour.getDailyItinerary() != null ?
                tour.getDailyItinerary().stream().map(this::toItineraryResponse).collect(Collectors.toList()) : new ArrayList<>();

        return new TourDetailResponse(
                tour.getId(),
                tour.getTitle(),
                tour.getSlug(),
                tour.getShortDescription(),
                tour.getDescription(),
                tour.getDifficulty(),
                tour.getDurationDays(),
                tour.getDurationNights(),
                tour.getDistanceKm(),
                tour.getMaxElevationM(),
                tour.getStartLocation(),
                tour.getEndLocation(),
                tour.getStartLat(),
                tour.getStartLng(),
                tour.getEndLat(),
                tour.getEndLng(),
                tour.getRouteGpxUrl(),
                tour.getHighlights(),
                tour.getIncludes(),
                tour.getExcludes(),
                tour.getRequirements(),
                tour.getStatus(),
                tour.getAvgRating(),
                tour.getTotalReviews(),
                (int) departureRepository.countByTourId(tour.getId()),
                (int) bookingRepository.countByDepartureTourId(tour.getId()),
                tour.getCreatedAt(),
                tour.getUpdatedAt(),
                images,
                waypoints,
                dailyItinerary
        );
    }

    public TourWaypointResponse toWaypointResponse(TourWaypoint wp) {
        if (wp == null) return null;
        return new TourWaypointResponse(
                wp.getId(),
                wp.getName(),
                wp.getSlug(),
                wp.getSequenceOrder(),
                wp.getWaypointType(),
                wp.getLat(),
                wp.getLng(),
                wp.getElevationM(),
                wp.getDayNumber(),
                wp.getIsDayEnd(),
                wp.getDescription(),
                wp.getNotesForGuide(),
                wp.getHasToilet(),
                wp.getHasShelter(),
                wp.getHasPhoneSignal(),
                wp.getHasFirstAid(),
                wp.getWaterSource(),
                wp.getWaterNotes(),
                wp.getAccommodation(),
                wp.getCampsiteCapacity(),
                wp.getCampsiteFeeVnd(),
                wp.getResupplyNotes(),
                wp.getEmergencyPhone(),
                wp.getEvacuationRouteNotes(),
                wp.getNearestHospital(),
                wp.getHospitalDistanceKm(),
                wp.getHelicopterLanding(),
                wp.getImageUrl(),
                wp.getThumbnailUrl(),
                wp.getIsActive(),
                wp.getLastVerifiedAt(),
                wp.getVerifiedBy()
        );
    }

    public TourImageResponse toImageResponse(TourImage image) {
        if (image == null) return null;
        return new TourImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getCaption(),
                image.getAltText(),
                image.getIsCover(),
                image.getSortOrder(),
                image.getCreatedAt()
        );
    }

    public TourDailyItineraryResponse toItineraryResponse(TourDailyItinerary itin) {
        if (itin == null) return null;

        List<ItineraryWaypointResponse> links = itin.getWaypointLinks() != null ?
                itin.getWaypointLinks().stream().map(this::toItineraryWaypointResponse).collect(Collectors.toList()) : new ArrayList<>();

        return new TourDailyItineraryResponse(
                itin.getId(),
                itin.getDayNumber(),
                itin.getDayTitle(),
                itin.getDayDescription(),
                itin.getStartWaypoint() != null ? itin.getStartWaypoint().getId() : null,
                itin.getStartWaypoint() != null ? itin.getStartWaypoint().getName() : null,
                itin.getEndWaypoint() != null ? itin.getEndWaypoint().getId() : null,
                itin.getEndWaypoint() != null ? itin.getEndWaypoint().getName() : null,
                itin.getDistanceKm(),
                itin.getElevationGainM(),
                itin.getElevationLossM(),
                itin.getWalkingHoursMin(),
                itin.getWalkingHoursMax(),
                itin.getDayDifficulty(),
                itin.getSuggestedStartTime(),
                itin.getSuggestedEndTime(),
                itin.getMealsIncluded(),
                itin.getMealNotes(),
                itin.getOvernightWaypoint() != null ? itin.getOvernightWaypoint().getId() : null,
                itin.getOvernightWaypoint() != null ? itin.getOvernightWaypoint().getName() : null,
                itin.getOvernightNotes(),
                itin.getSafetyNotes(),
                itin.getGuideNotes(),
                itin.getSortOrder(),
                links
        );
    }

    public ItineraryWaypointResponse toItineraryWaypointResponse(ItineraryWaypoint iw) {
        if (iw == null) return null;
        return new ItineraryWaypointResponse(
                iw.getId(),
                iw.getWaypoint() != null ? iw.getWaypoint().getId() : null,
                iw.getWaypoint() != null ? iw.getWaypoint().getName() : null,
                iw.getVisitOrder(),
                iw.getIsMandatory(),
                iw.getVisitNotes(),
                iw.getEstimatedArrival()
        );
    }

    public Tour toTour(TourRequest req) {
        if (req == null) return null;
        Tour tour = new Tour();
        updateTourFields(req, tour);
        return tour;
    }

    public void updateTour(TourRequest req, Tour tour) {
        if (req == null || tour == null) return;
        updateTourFields(req, tour);
    }

    private void updateTourFields(TourRequest req, Tour tour) {
        tour.setTitle(req.title());
        if (req.slug() != null) {
            tour.setSlug(req.slug());
        }
        tour.setShortDescription(req.shortDescription());
        tour.setDescription(req.description());
        if (req.difficulty() != null) {
            tour.setDifficulty(req.difficulty());
        }
        tour.setDurationDays(req.durationDays());
        if (req.durationNights() != null) {
            tour.setDurationNights(req.durationNights());
        }
        tour.setDistanceKm(req.distanceKm());
        tour.setMaxElevationM(req.maxElevationM());
        tour.setStartLocation(req.startLocation());
        tour.setEndLocation(req.endLocation());
        tour.setStartLat(req.startLat());
        tour.setStartLng(req.startLng());
        tour.setEndLat(req.endLat());
        tour.setEndLng(req.endLng());
        tour.setRouteGpxUrl(req.routeGpxUrl());
        
        List<TourAttribute> updatedAttributes = new ArrayList<>();
        if (req.highlights() != null) {
            for (String h : req.highlights()) {
                if (h == null || h.trim().isEmpty()) continue;
                TourAttribute attr = tourAttributeRepository.findByTypeAndContentIgnoreCase(TourAttributeType.HIGHLIGHT, h.trim())
                        .orElseGet(() -> tourAttributeRepository.save(
                                TourAttribute.builder().type(TourAttributeType.HIGHLIGHT).content(h.trim()).build()
                        ));
                updatedAttributes.add(attr);
            }
        }
        if (req.includes() != null) {
            for (String i : req.includes()) {
                if (i == null || i.trim().isEmpty()) continue;
                TourAttribute attr = tourAttributeRepository.findByTypeAndContentIgnoreCase(TourAttributeType.INCLUDE, i.trim())
                        .orElseGet(() -> tourAttributeRepository.save(
                                TourAttribute.builder().type(TourAttributeType.INCLUDE).content(i.trim()).build()
                        ));
                updatedAttributes.add(attr);
            }
        }
        if (req.excludes() != null) {
            for (String e : req.excludes()) {
                if (e == null || e.trim().isEmpty()) continue;
                TourAttribute attr = tourAttributeRepository.findByTypeAndContentIgnoreCase(TourAttributeType.EXCLUDE, e.trim())
                        .orElseGet(() -> tourAttributeRepository.save(
                                TourAttribute.builder().type(TourAttributeType.EXCLUDE).content(e.trim()).build()
                        ));
                updatedAttributes.add(attr);
            }
        }
        if (req.requirements() != null) {
            for (String r : req.requirements()) {
                if (r == null || r.trim().isEmpty()) continue;
                TourAttribute attr = tourAttributeRepository.findByTypeAndContentIgnoreCase(TourAttributeType.REQUIREMENT, r.trim())
                        .orElseGet(() -> tourAttributeRepository.save(
                                TourAttribute.builder().type(TourAttributeType.REQUIREMENT).content(r.trim()).build()
                        ));
                updatedAttributes.add(attr);
            }
        }
        tour.setAttributes(updatedAttributes);
        if (req.status() != null) {
            tour.setStatus(req.status());
        }
    }

    public TourWaypoint toWaypoint(TourWaypointRequest req, Tour tour) {
        if (req == null) return null;
        TourWaypoint wp = new TourWaypoint();
        wp.setTour(tour);
        updateWaypointFields(req, wp);
        return wp;
    }

    public void updateWaypoint(TourWaypointRequest req, TourWaypoint wp) {
        if (req == null || wp == null) return;
        updateWaypointFields(req, wp);
    }

    private void updateWaypointFields(TourWaypointRequest req, TourWaypoint wp) {
        wp.setName(req.name());
        wp.setSequenceOrder(req.sequenceOrder());
        if (req.waypointType() != null) {
            wp.setWaypointType(req.waypointType());
        }
        wp.setLat(req.lat());
        wp.setLng(req.lng());
        wp.setElevationM(req.elevationM());
        wp.setDayNumber(req.dayNumber());
        if (req.isDayEnd() != null) {
            wp.setIsDayEnd(req.isDayEnd());
        }
        wp.setDescription(req.description());
        wp.setNotesForGuide(req.notesForGuide());
        if (req.hasToilet() != null) wp.setHasToilet(req.hasToilet());
        if (req.hasShelter() != null) wp.setHasShelter(req.hasShelter());
        if (req.hasPhoneSignal() != null) wp.setHasPhoneSignal(req.hasPhoneSignal());
        if (req.hasFirstAid() != null) wp.setHasFirstAid(req.hasFirstAid());
        if (req.waterSource() != null) wp.setWaterSource(req.waterSource());
        wp.setWaterNotes(req.waterNotes());
        wp.setAccommodation(req.accommodation());
        wp.setCampsiteCapacity(req.campsiteCapacity());
        wp.setCampsiteFeeVnd(req.campsiteFeeVnd());
        wp.setResupplyNotes(req.resupplyNotes());
        wp.setEmergencyPhone(req.emergencyPhone());
        wp.setEvacuationRouteNotes(req.evacuationRouteNotes());
        wp.setNearestHospital(req.nearestHospital());
        wp.setHospitalDistanceKm(req.hospitalDistanceKm());
        if (req.helicopterLanding() != null) wp.setHelicopterLanding(req.helicopterLanding());
        wp.setImageUrl(req.imageUrl());
        wp.setThumbnailUrl(req.thumbnailUrl());
        if (req.isActive() != null) wp.setIsActive(req.isActive());
    }

    public TourDailyItinerary toItinerary(TourDailyItineraryRequest req, Tour tour) {
        if (req == null) return null;
        TourDailyItinerary itin = new TourDailyItinerary();
        itin.setTour(tour);
        updateItineraryFields(req, itin);
        return itin;
    }

    public void updateItinerary(TourDailyItineraryRequest req, TourDailyItinerary itin) {
        if (req == null || itin == null) return;
        updateItineraryFields(req, itin);
    }

    private void updateItineraryFields(TourDailyItineraryRequest req, TourDailyItinerary itin) {
        itin.setDayNumber(req.dayNumber());
        itin.setDayTitle(req.dayTitle());
        itin.setDayDescription(req.dayDescription());
        itin.setDistanceKm(req.distanceKm());
        itin.setElevationGainM(req.elevationGainM());
        itin.setElevationLossM(req.elevationLossM());
        itin.setWalkingHoursMin(req.walkingHoursMin());
        itin.setWalkingHoursMax(req.walkingHoursMax());
        itin.setDayDifficulty(req.dayDifficulty());
        itin.setSuggestedStartTime(req.suggestedStartTime());
        itin.setSuggestedEndTime(req.suggestedEndTime());
        itin.setMealsIncluded(req.mealsIncluded() != null ? req.mealsIncluded() : new ArrayList<>());
        itin.setMealNotes(req.mealNotes());
        itin.setOvernightNotes(req.overnightNotes());
        itin.setSafetyNotes(req.safetyNotes());
        itin.setGuideNotes(req.guideNotes());
        if (req.sortOrder() != null) {
            itin.setSortOrder(req.sortOrder());
        }

        // Resolving waypoint links (lazy resolution done in service layer because it needs repository lookup)
        if (req.startWaypointId() != null) {
            itin.setStartWaypoint(waypointRepository.findById(req.startWaypointId()).orElse(null));
        } else {
            itin.setStartWaypoint(null);
        }

        if (req.endWaypointId() != null) {
            itin.setEndWaypoint(waypointRepository.findById(req.endWaypointId()).orElse(null));
        } else {
            itin.setEndWaypoint(null);
        }

        if (req.overnightWaypointId() != null) {
            itin.setOvernightWaypoint(waypointRepository.findById(req.overnightWaypointId()).orElse(null));
        } else {
            itin.setOvernightWaypoint(null);
        }
    }
}
