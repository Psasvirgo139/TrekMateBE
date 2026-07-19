package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.BulkDepartureRequest;
import com.trekmate.backend.dto.request.TourDepartureRequest;
import com.trekmate.backend.dto.response.DepartureCardResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.model.enums.DepartureStatus;
import com.trekmate.backend.repository.DepartureGuideRepository;
import com.trekmate.backend.repository.DepartureWeatherDailyRepository;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.repository.TourRepository;
import com.trekmate.backend.repository.GuideRepository;
import com.trekmate.backend.service.impl.TourDepartureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourDepartureServiceImplTest {

    @Mock
    private TourRepository tourRepository;
    @Mock
    private TourDepartureRepository departureRepository;
    @Mock
    private DepartureGuideRepository departureGuideRepository;
    @Mock
    private DepartureWeatherDailyRepository weatherDailyRepository;
    @Mock
    private GuideRepository guideRepository;

    @InjectMocks
    private TourDepartureServiceImpl departureService;

    private UUID tourId;
    private Tour tour;

    @BeforeEach
    void setUp() {
        tourId = UUID.randomUUID();
        tour = Tour.builder()
                .title("Test Tour")
                .slug("test-tour")
                .durationDays((short) 3)
                .durationNights((short) 2)
                .build();
        tour.setId(tourId);
    }

    @Test
    void getTourDepartures_whenTourNotFound_throwsException() {
        when(tourRepository.existsById(tourId)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> 
                departureService.getTourDepartures(tourId, PageRequest.of(0, 10)));
        assertEquals(ErrorCode.TOUR_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getTourDepartures_success() {
        when(tourRepository.existsById(tourId)).thenReturn(true);
        TourDeparture dep = TourDeparture.builder()
                .tour(tour)
                .departureDate(LocalDate.now())
                .returnDate(LocalDate.now().plusDays(2))
                .pricePerPerson(BigDecimal.valueOf(100))
                .maxGroupSize((short) 10)
                .status(DepartureStatus.SCHEDULED)
                .build();
        dep.setId(UUID.randomUUID());

        when(departureRepository.findByTourId(eq(tourId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dep)));
        when(departureGuideRepository.findByDepartureId(dep.getId())).thenReturn(Collections.emptyList());
        when(weatherDailyRepository.findByDepartureIdOrderByDayNumberAsc(dep.getId())).thenReturn(Collections.emptyList());

        Page<DepartureCardResponse> result = departureService.getTourDepartures(tourId, PageRequest.of(0, 10));
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(dep.getId(), result.getContent().get(0).departureId());
    }

    @Test
    void createDeparture_whenDuplicateDate_throwsException() {
        LocalDate date = LocalDate.now();
        TourDepartureRequest req = new TourDepartureRequest(
                date, null, null, BigDecimal.valueOf(100), (short) 10, (short) 2, true, "Hanoi", null, null, "Notes", null, null
        );

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(tour));
        when(departureRepository.findByTourIdAndDepartureDate(tourId, date))
                .thenReturn(Optional.of(new TourDeparture()));

        AppException ex = assertThrows(AppException.class, () ->
                departureService.createDeparture(tourId, req));
        assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.getErrorCode());
    }

    @Test
    void createDeparture_successCalculatesDates() {
        LocalDate date = LocalDate.of(2026, 7, 10);
        // Tour durationDays is 3, so returnDate should be 2026-07-12
        // cutoffDate should default to 2026-07-09
        TourDepartureRequest req = new TourDepartureRequest(
                date, null, null, BigDecimal.valueOf(100), (short) 10, (short) 2, true, "Hanoi", null, null, "Notes", null, null
        );

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(tour));
        when(departureRepository.findByTourIdAndDepartureDate(tourId, date)).thenReturn(Optional.empty());

        when(departureRepository.saveAndFlush(any(TourDeparture.class))).thenAnswer(inv -> {
            TourDeparture dep = inv.getArgument(0);
            dep.setId(UUID.randomUUID());
            return dep;
        });

        DepartureCardResponse result = departureService.createDeparture(tourId, req);
        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 7, 12), result.returnDate());
        assertEquals(LocalDate.of(2026, 7, 9), result.cutoffDate());
        assertEquals(BigDecimal.valueOf(100), result.pricePerPerson());
    }

    @Test
    void generateBulkDepartures_success() {
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 15);
        // 2026-07-01 is Wednesday.
        // Let's filter SATURDAY and SUNDAY.
        // SAT: July 4, July 11
        // SUN: July 5, July 12
        BulkDepartureRequest req = new BulkDepartureRequest(
                startDate, endDate, List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                BigDecimal.valueOf(150), (short) 12, (short) 3, true, "Saigon", null, null, "Notes", null
        );

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(tour));
        // Mock findByTourIdAndDepartureDate to always return empty
        when(departureRepository.findByTourIdAndDepartureDate(eq(tourId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        when(departureRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<TourDeparture> list = inv.getArgument(0);
            list.forEach(dep -> dep.setId(UUID.randomUUID()));
            return list;
        });

        List<DepartureCardResponse> result = departureService.generateBulkDepartures(tourId, req);
        assertNotNull(result);
        // Should have 4 generated departures: July 4, July 5, July 11, July 12
        assertEquals(4, result.size());

        // Verify the generated dates
        assertTrue(result.stream().anyMatch(d -> d.departureDate().equals(LocalDate.of(2026, 7, 4))));
        assertTrue(result.stream().anyMatch(d -> d.departureDate().equals(LocalDate.of(2026, 7, 5))));
        assertTrue(result.stream().anyMatch(d -> d.departureDate().equals(LocalDate.of(2026, 7, 11))));
        assertTrue(result.stream().anyMatch(d -> d.departureDate().equals(LocalDate.of(2026, 7, 12))));

        // For July 4 departure, return date should be July 6 (4 + 3 - 1)
        DepartureCardResponse july4Dep = result.stream()
                .filter(d -> d.departureDate().equals(LocalDate.of(2026, 7, 4)))
                .findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 7, 6), july4Dep.returnDate());
        assertEquals(LocalDate.of(2026, 7, 3), july4Dep.cutoffDate());
    }

    @Test
    void createDeparture_whenGuideConflict_throwsException() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        UUID guideId = UUID.randomUUID();
        TourDepartureRequest req = new TourDepartureRequest(
                date, null, null, BigDecimal.valueOf(100), (short) 10, (short) 2, true, "Hanoi", null, null, "Notes", List.of(guideId), null
        );

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(tour));
        when(departureRepository.findByTourIdAndDepartureDate(tourId, date)).thenReturn(Optional.empty());

        com.trekmate.backend.model.Guide mockGuide = new com.trekmate.backend.model.Guide();
        mockGuide.setId(guideId);
        mockGuide.setDisplayName("John Doe");
        mockGuide.setIsAvailable(true);
        com.trekmate.backend.model.User mockUser = new com.trekmate.backend.model.User();
        mockUser.setIsActive(true);
        mockGuide.setUser(mockUser);

        when(guideRepository.findById(guideId)).thenReturn(Optional.of(mockGuide));
        
        // Mock conflict returned by repository
        com.trekmate.backend.model.DepartureGuide conflict = new com.trekmate.backend.model.DepartureGuide();
        when(departureGuideRepository.findConflicts(eq(guideId), any(), any(), any()))
                .thenReturn(List.of(conflict));

        when(departureRepository.saveAndFlush(any(TourDeparture.class))).thenAnswer(inv -> {
            TourDeparture dep = inv.getArgument(0);
            dep.setId(UUID.randomUUID());
            return dep;
        });

        AppException ex = assertThrows(AppException.class, () ->
                departureService.createDeparture(tourId, req));
        assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("conflict"));
    }
}
