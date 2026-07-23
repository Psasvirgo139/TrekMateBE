package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.response.AdminDashboardStatsResponse;
import com.trekmate.backend.model.Booking;
import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.enums.BookingStatus;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.repository.BookingRepository;
import com.trekmate.backend.repository.TourRepository;
import com.trekmate.backend.repository.UserRepository;
import com.trekmate.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPreviousMonth = startOfCurrentMonth.minusNanos(1);

        // 1. Overview Stats
        BigDecimal totalRevenue = bookingRepository.sumTotalRevenue();
        BigDecimal currentMonthRevenue = bookingRepository.sumRevenueBetween(startOfCurrentMonth, now);
        BigDecimal previousMonthRevenue = bookingRepository.sumRevenueBetween(startOfPreviousMonth, endOfPreviousMonth);
        double revenuePercentageChange = calculatePercentageChange(currentMonthRevenue, previousMonthRevenue);

        long totalBookings = bookingRepository.count();
        long currentMonthBookings = bookingRepository.countByBookedAtBetween(startOfCurrentMonth, now);
        long previousMonthBookings = bookingRepository.countByBookedAtBetween(startOfPreviousMonth, endOfPreviousMonth);
        double bookingsPercentageChange = calculatePercentageChange(currentMonthBookings, previousMonthBookings);

        long totalUsers = userRepository.count();
        long currentMonthUsers = userRepository.countByCreatedAtBetween(startOfCurrentMonth, now);
        long previousMonthUsers = userRepository.countByCreatedAtBetween(startOfPreviousMonth, endOfPreviousMonth);
        double usersPercentageChange = calculatePercentageChange(currentMonthUsers, previousMonthUsers);

        long totalTours = tourRepository.countByStatus(TourStatus.ACTIVE);
        long currentMonthTours = tourRepository.countByCreatedAtBetween(startOfCurrentMonth, now);
        long previousMonthTours = tourRepository.countByCreatedAtBetween(startOfPreviousMonth, endOfPreviousMonth);
        double toursPercentageChange = calculatePercentageChange(currentMonthTours, previousMonthTours);

        AdminDashboardStatsResponse.OverviewStats overview = new AdminDashboardStatsResponse.OverviewStats(
                totalRevenue,
                totalBookings,
                totalUsers,
                totalTours,
                revenuePercentageChange,
                bookingsPercentageChange,
                usersPercentageChange,
                toursPercentageChange
        );

        // 2. Booking Status Breakdown
        List<Object[]> statusCounts = bookingRepository.countBookingsGroupByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            statusMap.put(status.name(), 0L);
        }
        for (Object[] row : statusCounts) {
            if (row[0] != null) {
                statusMap.put(((BookingStatus) row[0]).name(), (Long) row[1]);
            }
        }

        // 3. Revenue Trend (last 6 months)
        List<AdminDashboardStatsResponse.MonthlyRevenueTrend> revenueTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = startOfCurrentMonth.minusMonths(i);
            LocalDateTime monthEnd = (i == 0) ? now : monthStart.plusMonths(1).minusNanos(1);

            BigDecimal revenue = bookingRepository.sumRevenueBetween(monthStart, monthEnd);
            long bookingsCount = bookingRepository.countByBookedAtBetween(monthStart, monthEnd);

            String monthLabel = monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue());
            revenueTrend.add(new AdminDashboardStatsResponse.MonthlyRevenueTrend(monthLabel, revenue, bookingsCount));
        }

        // 4. Popular Tours (top 5)
        List<Object[]> popularToursData = bookingRepository.findPopularTours(PageRequest.of(0, 5));
        List<AdminDashboardStatsResponse.PopularTourStats> popularTours = popularToursData.stream().map(row -> {
            Tour tour = (Tour) row[0];
            long bookingsCount = (long) row[1];
            BigDecimal totalRev = (BigDecimal) row[2];
            double avgRating = tour.getAvgRating() != null ? tour.getAvgRating().doubleValue() : 0.0;
            return new AdminDashboardStatsResponse.PopularTourStats(
                    tour.getId(),
                    tour.getTitle(),
                    bookingsCount,
                    totalRev,
                    avgRating
            );
        }).toList();

        // 5. Recent Bookings (top 5)
        List<Booking> recentBookingsList = bookingRepository.findTop5ByOrderByBookedAtDesc();
        List<AdminDashboardStatsResponse.RecentBookingDto> recentBookings = recentBookingsList.stream().map(b -> {
            String customerName = b.getUser().getCustomer() != null ? b.getUser().getCustomer().getFullName() : b.getUser().getEmail();
            String tourTitle = b.getDeparture() != null && b.getDeparture().getTour() != null ? b.getDeparture().getTour().getTitle() : "N/A";
            return new AdminDashboardStatsResponse.RecentBookingDto(
                    b.getBookingCode(),
                    customerName,
                    tourTitle,
                    b.getBookedAt(),
                    b.getTotalPrice(),
                    b.getStatus().name()
            );
        }).toList();

        return new AdminDashboardStatsResponse(
                overview,
                statusMap,
                revenueTrend,
                popularTours,
                recentBookings
        );
    }

    private double calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private double calculatePercentageChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100.0;
    }
}
