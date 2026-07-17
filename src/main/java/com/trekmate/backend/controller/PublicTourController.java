package com.trekmate.backend.controller;

import com.trekmate.backend.dto.response.*;
import com.trekmate.backend.model.enums.DepartureStatus;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.repository.TourRepository;
import com.trekmate.backend.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller công khai — chỉ chứa các GET endpoint không cần xác thực.
 * Mapping: /api/tours/**
 * Security: permitAll() cho toàn bộ GET
 */
@RestController
@Validated
@Slf4j(topic = "API-PUBLIC-TOUR-CONTROLLER")
@RequestMapping("/tours")
@RequiredArgsConstructor
@Tag(name = "Tours (Public)", description = "Tra cứu công khai thông tin tour trekking")
public class PublicTourController {

    private final TourService tourService;
    private final TourDepartureRepository departureRepository;
    private final TourRepository tourRepository;

    // ────────────────────────────────────────────────────────────────────────────
    // Listing / Search (public)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Dành cho USER — chỉ trả tour có trạng thái ACTIVE.
     * GET /api/tours
     */
    @GetMapping
    @Operation(
            summary = "Lấy danh sách tour (user)",
            description = "Trả về danh sách tour đang hoạt động (ACTIVE) có phân trang, hỗ trợ tìm kiếm và lọc theo độ khó, khoảng thời gian."
    )
    public ApiResponse<Page<TourCardResponse>> getToursForUser(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) Short minDuration,
            @RequestParam(required = false) Short maxDuration,
            Pageable pageable) {
        log.info("REST [USER] get tours: search='{}', difficulty='{}'", search, difficulty);
        Page<TourCardResponse> data = tourService.getTours(
                search, difficulty, TourStatus.ACTIVE, minDuration, maxDuration, pageable);
        return ApiResponse.<Page<TourCardResponse>>builder()
                .code(200)
                .message("Lấy danh sách tour thành công")
                .data(data)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Tour Detail (public)
    // ────────────────────────────────────────────────────────────────────────────

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Lấy chi tiết tour", description = "Lấy chi tiết tour (kèm waypoints, daily itineraries, images) qua UUID hoặc slug")
    public ResponseEntity<TourDetailResponse> getTourByIdOrSlug(@PathVariable String idOrSlug) {
        log.info("REST request to get tour detail: {}", idOrSlug);
        TourDetailResponse dto = tourService.getTourByIdOrSlug(idOrSlug);
        return ResponseEntity.ok(dto);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Departures (public — for booking widget)
    // ────────────────────────────────────────────────────────────────────────────

    @GetMapping("/{idOrSlug}/departures")
    @Operation(summary = "Lấy danh sách đợt khởi hành của tour",
               description = "Trả về các đợt khởi hành còn OPEN của tour. Dùng cho widget đặt tour ở trang chi tiết.")
    public ApiResponse<List<Map<String, Object>>> getDeparturesByTour(
            @PathVariable String idOrSlug) {
        log.info("REST request to get departures for tour: {}", idOrSlug);

        // Resolve UUID or slug
        UUID tourId;
        try {
            tourId = UUID.fromString(idOrSlug);
        } catch (IllegalArgumentException e) {
            tourId = tourRepository.findBySlug(idOrSlug)
                    .map(t -> t.getId())
                    .orElse(null);
        }
        if (tourId == null) {
            return ApiResponse.<List<Map<String, Object>>>builder()
                    .code(404)
                    .message("Không tìm thấy tour")
                    .build();
        }

        final UUID finalTourId = tourId;
        List<Map<String, Object>> result = departureRepository
                .findByTourIdAndStatus(finalTourId, DepartureStatus.OPEN)
                .stream()
                .filter(d -> d.getDepartureDate() != null && !d.getDepartureDate().isBefore(LocalDate.now()))
                .map(d -> {
                    int availableSlots = (d.getMaxGroupSize() == null ? 0 : d.getMaxGroupSize())
                            - (d.getBookedSlots() == null ? 0 : d.getBookedSlots());
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id",             d.getId().toString());
                    m.put("departureId",    d.getId().toString());
                    m.put("departureDate",  d.getDepartureDate().toString());
                    m.put("returnDate",     d.getReturnDate() != null ? d.getReturnDate().toString() : "");
                    m.put("cutoffDate",     d.getCutoffDate() != null ? d.getCutoffDate().toString() : "");
                    m.put("pricePerPerson", d.getPricePerPerson() != null ? d.getPricePerPerson() : BigDecimal.ZERO);
                    m.put("availableSlots", availableSlots);
                    m.put("meetingPoint",   d.getMeetingPoint() != null ? d.getMeetingPoint() : "");
                    m.put("allowJoinTour",  d.getAllowJoinTour() != null && d.getAllowJoinTour());
                    m.put("status",         d.getStatus() != null ? d.getStatus().toString() : "");
                    return m;
                })
                .collect(Collectors.toList());

        return ApiResponse.<List<Map<String, Object>>>builder()
                .code(200)
                .message("Lấy danh sách đợt khởi hành thành công")
                .data(result)
                .build();
    }
}
