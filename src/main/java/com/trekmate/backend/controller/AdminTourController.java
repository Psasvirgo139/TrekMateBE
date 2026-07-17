package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.*;
import com.trekmate.backend.dto.response.*;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller quản trị — chứa tất cả write operations và admin-only GET.
 * Mapping: /api/admin/tours/**
 * Security: hasAnyRole("GUIDE", "ADMIN")
 */
@RestController
@Validated
@Slf4j(topic = "API-ADMIN-TOUR-CONTROLLER")
@RequestMapping("/admin/tours")
@RequiredArgsConstructor
@Tag(name = "Tours (Admin)", description = "Quản lý tour trekking dành cho Admin và Guide")
public class AdminTourController {

    private final TourService tourService;

    // ────────────────────────────────────────────────────────────────────────────
    // Admin Listing
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Dành cho ADMIN — trả tất cả tour không phân biệt trạng thái.
     * GET /api/admin/tours/all
     */
    @GetMapping("/all")
    @Operation(
            summary = "Lấy tất cả tour (admin)",
            description = "Trả về toàn bộ tour có phân trang, hỗ trợ lọc theo từ khóa, độ khó và trạng thái (DRAFT / ACTIVE / ARCHIVED …)."
    )
    public ApiResponse<Page<TourDetailResponse>> getToursForAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) TourStatus status,
            Pageable pageable) {
        log.info("REST [ADMIN] get all tours: search='{}', difficulty='{}', status='{}'", search, difficulty, status);
        Page<TourDetailResponse> data = tourService.getAllTours(search, difficulty, status, pageable);
        return ApiResponse.<Page<TourDetailResponse>>builder()
                .code(200)
                .message("Lấy danh sách tour thành công")
                .data(data)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Tour Detail / CRUD
    // ────────────────────────────────────────────────────────────────────────────

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Lấy chi tiết tour (admin)", description = "Lấy chi tiết tour qua UUID hoặc slug. Dành cho trang chỉnh sửa tour của admin/guide.")
    public ResponseEntity<TourDetailResponse> getTourByIdOrSlug(@PathVariable String idOrSlug) {
        log.info("REST [ADMIN] request to get tour detail: {}", idOrSlug);
        TourDetailResponse dto = tourService.getTourByIdOrSlug(idOrSlug);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/clone-source")
    @Operation(summary = "Lấy dữ liệu nguồn để nhân bản tour", description = "Trả về chi tiết tour nhưng xóa các trường ID, slug, rating và đánh giá để chuẩn bị cho việc tạo mới pre-fill.")
    public ResponseEntity<TourDetailResponse> getTourForClone(@PathVariable UUID id) {
        log.info("REST request to get tour clone source: {}", id);
        TourDetailResponse dto = tourService.getTourForClone(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Operation(summary = "Tạo tour mới", description = "Tạo một tour mới. Slug sẽ tự động được tạo từ title nếu để trống.")
    public ResponseEntity<TourDetailResponse> createTour(@Valid @RequestBody TourRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.createTour(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tour", description = "Cập nhật thông tin chi tiết cơ bản của một tour.")
    public ResponseEntity<TourDetailResponse> updateTour(
            @PathVariable UUID id,
            @Valid @RequestBody TourRequest request) {
        return ResponseEntity.ok(tourService.updateTour(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tour", description = "Xóa tour theo ID. Nếu tour đã có liên kết lịch khởi hành hoặc đơn đặt, hệ thống sẽ tự động cập nhật trạng thái tour về ARCHIVED (Soft Delete).")
    public ResponseEntity<Void> deleteTour(@PathVariable UUID id) {
        tourService.deleteTour(id);
        return ResponseEntity.noContent().build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Waypoints CRUD
    // ────────────────────────────────────────────────────────────────────────────

    @PostMapping("/{tourId}/waypoints")
    @Operation(summary = "Thêm waypoint mới", description = "Thêm điểm mốc (waypoint) mới cho tour.")
    public ResponseEntity<TourWaypointResponse> addWaypoint(
            @PathVariable UUID tourId,
            @Valid @RequestBody TourWaypointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.addWaypoint(tourId, request));
    }

    @PutMapping("/{tourId}/waypoints/{waypointId}")
    @Operation(summary = "Cập nhật waypoint", description = "Cập nhật thông tin điểm mốc trong tour.")
    public ResponseEntity<TourWaypointResponse> updateWaypoint(
            @PathVariable UUID tourId,
            @PathVariable UUID waypointId,
            @Valid @RequestBody TourWaypointRequest request) {
        return ResponseEntity.ok(tourService.updateWaypoint(tourId, waypointId, request));
    }

    @DeleteMapping("/{tourId}/waypoints/{waypointId}")
    @Operation(summary = "Xóa waypoint", description = "Xóa điểm mốc khỏi tour.")
    public ResponseEntity<Void> deleteWaypoint(
            @PathVariable UUID tourId,
            @PathVariable UUID waypointId) {
        tourService.deleteWaypoint(tourId, waypointId);
        return ResponseEntity.noContent().build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Daily Itinerary CRUD
    // ────────────────────────────────────────────────────────────────────────────

    @PostMapping("/{tourId}/itineraries")
    @Operation(summary = "Thêm hoặc Cập nhật lịch trình ngày", description = "Thêm lịch trình cho ngày mới hoặc cập nhật ngày đã có dựa trên day_number.")
    public ResponseEntity<TourDailyItineraryResponse> addOrUpdateDailyItinerary(
            @PathVariable UUID tourId,
            @Valid @RequestBody TourDailyItineraryRequest request) {
        return ResponseEntity.ok(tourService.addOrUpdateDailyItinerary(tourId, request));
    }

    @DeleteMapping("/{tourId}/itineraries/{itineraryId}")
    @Operation(summary = "Xóa lịch trình ngày", description = "Xóa lịch trình một ngày cụ thể của tour.")
    public ResponseEntity<Void> deleteDailyItinerary(
            @PathVariable UUID tourId,
            @PathVariable UUID itineraryId) {
        tourService.deleteDailyItinerary(tourId, itineraryId);
        return ResponseEntity.noContent().build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Images CRUD
    // ────────────────────────────────────────────────────────────────────────────

    @PostMapping("/{tourId}/images")
    @Operation(summary = "Thêm ảnh cho tour", description = "Thêm ảnh mới vào bộ sưu tập hình ảnh của tour.")
    public ResponseEntity<TourImageResponse> addTourImage(
            @PathVariable UUID tourId,
            @Valid @RequestBody TourImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.addTourImage(tourId, request));
    }

    @DeleteMapping("/{tourId}/images/{imageId}")
    @Operation(summary = "Xóa ảnh của tour", description = "Xóa một ảnh khỏi bộ sưu tập hình ảnh của tour.")
    public ResponseEntity<Void> deleteTourImage(
            @PathVariable UUID tourId,
            @PathVariable Long imageId) {
        tourService.deleteTourImage(tourId, imageId);
        return ResponseEntity.noContent().build();
    }
}
