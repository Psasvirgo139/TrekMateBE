package com.trekmate.backend.controller;

import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.TourCardResponse;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/tours")
@Slf4j(topic = "API-TOUR-CONTROLLER")
@Tag(name = "Tours", description = "Tour du lịch — tìm kiếm, lọc và phân trang danh sách tour")
public class PublicTourController {

    private final TourService tourService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách tour",
            description = "Trả về danh sách tour có phân trang, hỗ trợ tìm kiếm theo từ khóa và lọc theo độ khó, trạng thái, khoảng thời gian."
    )
    public ApiResponse<Page<TourCardResponse>> getTours(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) TourStatus status,
            @RequestParam(required = false) Short minDuration,
            @RequestParam(required = false) Short maxDuration,
            Pageable pageable
    ) {
        log.info("REST request to get tours list with search='{}', difficulty='{}', status='{}'", search, difficulty, status);
        Page<TourCardResponse> data = tourService.getTours(search, difficulty, status, minDuration, maxDuration, pageable);
        return ApiResponse.<Page<TourCardResponse>>builder()
                .code(200)
                .message("Lấy danh sách tour thành công")
                .data(data)
                .build();
    }
}
