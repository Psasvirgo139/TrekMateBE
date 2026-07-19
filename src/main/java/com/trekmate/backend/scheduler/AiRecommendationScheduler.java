package com.trekmate.backend.scheduler;

import com.trekmate.backend.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AiRecommendationScheduler — Tác vụ nền chạy mỗi ngày lúc 01:00 AM giờ Việt Nam.
 *
 * Quy trình:
 *  1. WeatherScheduler chạy lúc 00:00 AM → cập nhật dự báo thời tiết 14 ngày từ Open-Meteo
 *  2. AiRecommendationScheduler chạy lúc 01:00 AM → đảm bảo thời tiết đã sẵn sàng
 *     → quét các TourDeparture khởi hành trong 10 ngày tới, chưa có AI recommendation cached
 *     → với mỗi departure: lấy thông tin thời tiết (đã lưu trong DB) + danh sách
 *        thiết bị cho thuê → gửi lên Gemini → lưu kết quả vào bảng departure_ai_recommendations
 *  3. Khi khách booking, hệ thống đọc recommendation thẳng từ DB — không gọi Gemini API
 *
 * zone = "Asia/Ho_Chi_Minh" đảm bảo lịch chạy theo giờ địa phương của server,
 * không phụ thuộc vào UTC mặc định của JVM.
 *
 * Cron: "0 0 1 * * *" = 01:00:00 mỗi ngày
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRecommendationScheduler {

    private final AiRecommendationService aiRecommendationService;

    /**
     * Chạy mỗi ngày lúc 01:00 AM (giờ Việt Nam) — tính trước AI recommendation
     * cho tất cả departure trong 10 ngày tiếp theo chưa có cache.
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Ho_Chi_Minh")
    public void dailyAiRecommendationPrecalculation() {
        log.info("[AiRecommendationScheduler] Starting daily AI gear recommendation pre-calculation (01:00 AM VN)");
        try {
            aiRecommendationService.precalculateAiRecommendations();
            log.info("[AiRecommendationScheduler] Daily pre-calculation completed successfully");
        } catch (Exception e) {
            log.error("[AiRecommendationScheduler] Pre-calculation failed: {}", e.getMessage(), e);
        }
    }
}
