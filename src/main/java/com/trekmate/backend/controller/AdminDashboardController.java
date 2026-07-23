package com.trekmate.backend.controller;

import com.trekmate.backend.dto.response.AdminDashboardStatsResponse;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Overview statistical endpoints for Admin users")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get overall dashboard stats (revenue, bookings, users, popular tours)")
    public ApiResponse<AdminDashboardStatsResponse> getDashboardStats() {
        AdminDashboardStatsResponse data = adminDashboardService.getDashboardStats();
        return ApiResponse.<AdminDashboardStatsResponse>builder()
                .code(200)
                .message("Get dashboard statistics successfully")
                .data(data)
                .build();
    }
}
