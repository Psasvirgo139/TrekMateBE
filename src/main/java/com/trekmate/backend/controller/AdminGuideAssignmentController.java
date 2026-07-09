package com.trekmate.backend.controller;

import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.AvailableGuideResponse;
import com.trekmate.backend.dto.response.GuideScheduleResponse;
import com.trekmate.backend.service.TourDepartureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@Slf4j(topic = "API-ADMIN-GUIDE-ASSIGNMENT-CONTROLLER")
@RequestMapping("/admin/tour-guides")
@RequiredArgsConstructor
@Tag(name = "Admin Guide Assignments", description = "Quản lý và tra cứu lịch biểu phân công Hướng dẫn viên")
public class AdminGuideAssignmentController {

    private final TourDepartureService departureService;

    @GetMapping("/available")
    @Operation(summary = "Lấy danh sách HDV rảnh", description = "Trả về danh sách các HDV có trạng thái Active và không bị trùng bất kỳ lịch trình tour nào trong khoảng ngày được chọn.")
    public ApiResponse<List<AvailableGuideResponse>> getAvailableGuides(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) java.util.UUID excludeDepartureId) {
        log.info("REST [ADMIN] get available guides from {} to {} excluding departure {}", startDate, endDate, excludeDepartureId);
        List<AvailableGuideResponse> data = departureService.getAvailableGuides(startDate, endDate, excludeDepartureId);
        return ApiResponse.<List<AvailableGuideResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách HDV khả dụng thành công")
                .data(data)
                .build();
    }

    @GetMapping("/schedules")
    @Operation(summary = "Lấy tất cả phân công HDV", description = "Trả về danh sách tất cả các lịch đi tour của tất cả HDV trong khoảng ngày để hiển thị lên bảng điều phối (Calendar).")
    public ApiResponse<List<GuideScheduleResponse>> getGuideSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST [ADMIN] get guide schedules from {} to {}", startDate, endDate);
        List<GuideScheduleResponse> data = departureService.getGuideSchedules(startDate, endDate);
        return ApiResponse.<List<GuideScheduleResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy lịch trình phân công HDV thành công")
                .data(data)
                .build();
    }
}
