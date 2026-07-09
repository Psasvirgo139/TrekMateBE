package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.BulkDepartureRequest;
import com.trekmate.backend.dto.request.TourDepartureRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.DepartureCardResponse;
import com.trekmate.backend.service.TourDepartureService;
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

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@Slf4j(topic = "API-TOUR-DEPARTURE-CONTROLLER")
@RequestMapping("/admin/tours/{tourId}/departures")
@RequiredArgsConstructor
@Tag(name = "Tour Departures", description = "Quản lý lịch khởi hành (Departures) của từng tour trekking")
public class TourDepartureController {

    private final TourDepartureService departureService;

    @GetMapping
    @Operation(summary = "Lấy danh sách lịch khởi hành của tour", description = "Trả về danh sách tất cả các chuyến đi/lịch khởi hành của một tour cụ thể.")
    public ApiResponse<Page<DepartureCardResponse>> getTourDepartures(
            @PathVariable UUID tourId,
            Pageable pageable) {
        log.info("REST [ADMIN] get departures for tour ID: {}", tourId);
        Page<DepartureCardResponse> data = departureService.getTourDepartures(tourId, pageable);
        return ApiResponse.<Page<DepartureCardResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách lịch khởi hành thành công")
                .data(data)
                .build();
    }

    @PostMapping
    @Operation(summary = "Tạo một lịch khởi hành đơn lẻ", description = "Tạo một chuyến đi cụ thể cho tour vào ngày được chỉ định. Ngày về và hạn đặt chỗ tự động tính nếu không truyền.")
    public ResponseEntity<ApiResponse<DepartureCardResponse>> createDeparture(
            @PathVariable UUID tourId,
            @Valid @RequestBody TourDepartureRequest request) {
        log.info("REST [ADMIN] create single departure for tour ID: {} on date: {}", tourId, request.departureDate());
        DepartureCardResponse data = departureService.createDeparture(tourId, request);
        ApiResponse<DepartureCardResponse> response = ApiResponse.<DepartureCardResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo lịch khởi hành thành công")
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Tự động sinh lịch khởi hành lặp lại hàng loạt", description = "Sinh hàng loạt lịch khởi hành từ ngày bắt đầu đến ngày kết thúc theo các ngày trong tuần được chọn. Bỏ qua các ngày đã có lịch trùng.")
    public ResponseEntity<ApiResponse<List<DepartureCardResponse>>> generateBulkDepartures(
            @PathVariable UUID tourId,
            @Valid @RequestBody BulkDepartureRequest request) {
        log.info("REST [ADMIN] generate bulk departures for tour ID: {} from {} to {}", tourId, request.startDate(), request.endDate());
        List<DepartureCardResponse> data = departureService.generateBulkDepartures(tourId, request);
        ApiResponse<List<DepartureCardResponse>> response = ApiResponse.<List<DepartureCardResponse>>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo hàng loạt lịch khởi hành thành công")
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{departureId}")
    @Operation(summary = "Cập nhật thông tin lịch khởi hành", description = "Chỉnh sửa thông tin chi tiết của một chuyến đi/lịch khởi hành.")
    public ApiResponse<DepartureCardResponse> updateDeparture(
            @PathVariable UUID tourId,
            @PathVariable UUID departureId,
            @Valid @RequestBody TourDepartureRequest request) {
        log.info("REST [ADMIN] update departure ID: {} for tour ID: {}", departureId, tourId);
        DepartureCardResponse data = departureService.updateDeparture(tourId, departureId, request);
        return ApiResponse.<DepartureCardResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật lịch khởi hành thành công")
                .data(data)
                .build();
    }

    @DeleteMapping("/{departureId}")
    @Operation(summary = "Xóa hoặc hủy lịch khởi hành", description = "Xóa lịch khởi hành theo ID. Nếu chuyến đi đã có người đặt (bookings > 0), hệ thống tự động đổi trạng thái sang CANCELLED.")
    public ResponseEntity<Void> deleteDeparture(
            @PathVariable UUID tourId,
            @PathVariable UUID departureId) {
        log.info("REST [ADMIN] delete departure ID: {} for tour ID: {}", departureId, tourId);
        departureService.deleteDeparture(tourId, departureId);
        return ResponseEntity.noContent().build();
    }
}
