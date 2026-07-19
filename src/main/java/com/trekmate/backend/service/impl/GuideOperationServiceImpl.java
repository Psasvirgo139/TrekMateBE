package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.GuideAttendanceRequest;
import com.trekmate.backend.dto.response.GuideDepartureResponse;
import com.trekmate.backend.dto.response.GuideParticipantResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Booking;
import com.trekmate.backend.model.Customer;
import com.trekmate.backend.model.Guide;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.model.User;
import com.trekmate.backend.model.enums.BookingStatus;
import com.trekmate.backend.model.enums.DepartureStatus;
import com.trekmate.backend.repository.BookingRepository;
import com.trekmate.backend.repository.CustomerRepository;
import com.trekmate.backend.repository.DepartureGuideRepository;
import com.trekmate.backend.repository.GuideRepository;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.service.GuideOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideOperationServiceImpl implements GuideOperationService {

    private final TourDepartureRepository departureRepository;
    private final BookingRepository bookingRepository;
    private final DepartureGuideRepository departureGuideRepository;
    private final CustomerRepository customerRepository;
    private final GuideRepository guideRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GuideDepartureResponse> getGuideDepartures(UUID guideId) {
        log.info("Fetching departures assigned to guide ID: {}", guideId);
        return departureGuideRepository.findByGuideId(guideId).stream()
                .map(dg -> {
                    TourDeparture departure = dg.getDeparture();
                    int participantCount = bookingRepository.findByDepartureId(departure.getId()).stream()
                            .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                            .mapToInt(Booking::getNumParticipants)
                            .sum();

                    return new GuideDepartureResponse(
                            departure.getId(),
                            departure.getTour().getTitle(),
                            departure.getDepartureDate(),
                            departure.getReturnDate(),
                            departure.getStatus(),
                            participantCount
                    );
                })
                .sorted((a, b) -> b.departureDate().compareTo(a.departureDate()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideParticipantResponse> getDepartureParticipants(UUID departureId) {
        log.info("Fetching participants for departure ID: {}", departureId);
        return bookingRepository.findByDepartureId(departureId).stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(b -> {
                    User user = b.getUser();
                    String customerName = "Unknown";
                    String phone = "";
                    String email = "";

                    if (user != null) {
                        email = user.getEmail();
                        phone = user.getPhone();
                        if (user.getCustomer() != null) {
                            customerName = user.getCustomer().getFullName();
                        } else {
                            customerName = user.getEmail().split("@")[0];
                        }
                    }

                    return new GuideParticipantResponse(
                            b.getId(),
                            b.getBookingCode(),
                            customerName,
                            email,
                            phone,
                            b.getNumParticipants(),
                            b.getStatus()
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void startTour(UUID departureId, GuideAttendanceRequest request) {
        log.info("Starting tour for departure ID: {}", departureId);
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found"));

        LocalDate today = LocalDate.now();
        if (!today.isEqual(departure.getDepartureDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "You can only start the tour on its departure date: " + departure.getDepartureDate());
        }

        checkGuideOngoingTours(departure);

        departure.setStatus(DepartureStatus.ONGOING);
        departureRepository.save(departure);

        updateBookingsAttendance(request);
    }

    @Override
    @Transactional
    public void updateAttendance(UUID departureId, GuideAttendanceRequest request) {
        log.info("Updating attendance for departure ID: {}", departureId);
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found"));

        if (departure.getStatus() != DepartureStatus.ONGOING) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Attendance can only be edited during the ongoing departure period.");
        }

        checkGuideOngoingTours(departure);

        updateBookingsAttendance(request);
    }

    @Override
    @Transactional
    public void completeTour(UUID departureId) {
        log.info("Completing tour for departure ID: {}", departureId);
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found"));

        LocalDate today = LocalDate.now();
        if (today.isBefore(departure.getReturnDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Cannot mark tour as completed before its return date: " + departure.getReturnDate());
        }

        departure.setStatus(DepartureStatus.COMPLETED);
        departureRepository.save(departure);

        // Find all bookings for this departure
        List<Booking> bookings = bookingRepository.findByDepartureId(departureId);
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.ONGOING || booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);

                // Update totalToursJoined for the Customer
                User user = booking.getUser();
                if (user != null) {
                    customerRepository.findByUserId(user.getId()).ifPresent(customer -> {
                        long count = bookingRepository.countByUserIdAndStatus(user.getId(), BookingStatus.COMPLETED);
                        customer.setTotalToursJoined((int) count);
                        customerRepository.save(customer);
                        log.info("Updated totalToursJoined for customer {} to {}", customer.getFullName(), customer.getTotalToursJoined());
                    });
                }
            } else if (booking.getStatus() == BookingStatus.MISSING) {
                booking.setStatus(BookingStatus.MISSED);
                bookingRepository.save(booking);
                log.info("Transitioned booking {} from MISSING to MISSED", booking.getBookingCode());
            }
        }

        // Update totalToursLed for each assigned guide
        departureGuideRepository.findByDepartureId(departureId).forEach(dg -> {
            Guide guide = dg.getGuide();
            if (guide != null) {
                long count = departureGuideRepository.countByGuideIdAndDepartureStatus(guide.getId(), DepartureStatus.COMPLETED);
                guide.setTotalToursLed((int) count);
                guideRepository.save(guide);
                log.info("Updated totalToursLed for guide {} to {}", guide.getDisplayName(), guide.getTotalToursLed());
            }
        });
    }

    private void updateBookingsAttendance(GuideAttendanceRequest request) {
        if (request.presentBookingIds() != null) {
            for (Long bookingId : request.presentBookingIds()) {
                bookingRepository.findById(bookingId).ifPresent(b -> {
                    if (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.MISSING) {
                        b.setStatus(BookingStatus.ONGOING);
                        bookingRepository.save(b);
                    }
                });
            }
        }

        if (request.absentBookingIds() != null) {
            for (Long bookingId : request.absentBookingIds()) {
                bookingRepository.findById(bookingId).ifPresent(b -> {
                    if (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.ONGOING) {
                        b.setStatus(BookingStatus.MISSING);
                        bookingRepository.save(b);
                    }
                });
            }
        }
    }

    private void checkGuideOngoingTours(TourDeparture departure) {
        departureGuideRepository.findByDepartureId(departure.getId()).forEach(dg -> {
            Guide guide = dg.getGuide();
            if (guide != null) {
                List<TourDeparture> ongoingDepartures = departureGuideRepository.findByGuideId(guide.getId()).stream()
                        .map(dgItem -> dgItem.getDeparture())
                        .filter(d -> d.getStatus() == DepartureStatus.ONGOING && !d.getId().equals(departure.getId()))
                        .collect(Collectors.toList());
                if (!ongoingDepartures.isEmpty()) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, 
                            "Guide " + guide.getDisplayName() + " is already leading another ongoing tour: " 
                            + ongoingDepartures.get(0).getTour().getTitle() + ".");
                }
            }
        });
    }
}
