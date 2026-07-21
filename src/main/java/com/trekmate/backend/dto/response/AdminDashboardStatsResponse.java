package com.trekmate.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminDashboardStatsResponse(
        OverviewStats overview,
        Map<String, Long> bookingStatusBreakdown,
        List<MonthlyRevenueTrend> revenueTrend,
        List<PopularTourStats> popularTours,
        List<RecentBookingDto> recentBookings
) {
    public record OverviewStats(
            BigDecimal totalRevenue,
            long totalBookings,
            long totalUsers,
            long totalTours,
            double revenuePercentageChange,
            double bookingsPercentageChange,
            double usersPercentageChange,
            double toursPercentageChange
    ) {}

    public record MonthlyRevenueTrend(
            String month, // e.g., "2026-07"
            BigDecimal revenue,
            long bookingsCount
    ) {}

    public record PopularTourStats(
            UUID tourId,
            String title,
            long bookingsCount,
            BigDecimal totalRevenue,
            double avgRating
    ) {}

    public record RecentBookingDto(
            String bookingCode,
            String customerName,
            String tourTitle,
            LocalDateTime bookedAt,
            BigDecimal totalPrice,
            String status
    ) {}
}
