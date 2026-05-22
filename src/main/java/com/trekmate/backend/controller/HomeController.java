package com.trekmate.backend.controller;

import com.trekmate.backend.dto.response.HomeResponse;
import com.trekmate.backend.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "Trang chủ — dữ liệu tổng quan TrekMate")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    @Operation(
            summary = "Lấy dữ liệu trang chủ",
            description = "Trả về thống kê tổng quan, danh sách tour nổi bật, " +
                          "lịch khởi hành sắp tới và top HDV."
    )
    public ResponseEntity<HomeResponse> getHome() {
        return ResponseEntity.ok(homeService.getHomeData());
    }
}
