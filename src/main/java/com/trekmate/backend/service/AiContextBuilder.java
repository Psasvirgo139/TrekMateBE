package com.trekmate.backend.service;

import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.enums.TourStatus;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiContextBuilder {

    private final TourRepository tourRepository;
    private final TourDepartureRepository tourDepartureRepository;
    private final WeatherService weatherService;

    /**
     * Xay dung ngam context de cung cap cho Gemini (thong tin tour dang hoat dong, thoi tiet cac diem chinh, gia tour, link web).
     */
    public String buildSystemContext() {
        try {
            StringBuilder context = new StringBuilder();

            // 1. Dữ liệu tour hiện có (lấy tối đa 10 tour ACTIVE để tránh tràn context)
            context.append("--- DỮ LIỆU TOUR HIỆN CÓ ---\n");
            Page<Tour> activeTours = tourRepository.findByStatus(TourStatus.ACTIVE, PageRequest.of(0, 10));
            
            if (activeTours.isEmpty()) {
                context.append("Hiện tại không có tour nào đang mở.\n");
            } else {
                for (Tour tour : activeTours.getContent()) {
                    Optional<BigDecimal> minPriceOpt = tourDepartureRepository.findMinPriceByTourId(tour.getId());
                    String priceStr = minPriceOpt
                            .map(p -> String.format("%,.0f VNĐ/người", p.doubleValue()))
                            .orElse("Chưa có lịch khởi hành mở bán");

                    context.append(String.format("- Tên tour: %s (slug: %s)\n", tour.getTitle(), tour.getSlug()));
                    context.append(String.format("  Giá tour từ: %s\n", priceStr));
                    context.append(String.format("  Mức độ khó: %s, Thời gian: %s ngày %s đêm.\n", 
                            tour.getDifficulty(), tour.getDurationDays(), tour.getDurationNights()));
                    context.append(String.format("  Điểm xuất phát: %s, Kết thúc: %s\n", 
                            tour.getStartLocation(), tour.getEndLocation()));
                    context.append(String.format("  Link tour chi tiết: https://trek-mate-fe.vercel.app/tours/%s\n", tour.getSlug()));
                    
                    // Lấy thời tiết cho điểm xuất phát nếu có tọa độ (dự báo 7 ngày)
                    if (tour.getStartLat() != null && tour.getStartLng() != null) {
                        context.append("  Thời tiết tại điểm bắt đầu:\n");
                        String weather = weatherService.getForecastSummary(tour.getStartLat(), tour.getStartLng(), 7);
                        context.append("    ").append(weather.replace("\n", "\n    ")).append("\n");
                    }
                    context.append("\n");
                }
            }

            return context.toString();
        } catch (Exception e) {
            log.error("Error building AI context: {}", e.getMessage(), e);
            return "--- DỮ LIỆU TOUR HIỆN CÓ ---\nKhông thể tải dữ liệu tour lúc này do lỗi hệ thống.\n";
        }
    }
}
