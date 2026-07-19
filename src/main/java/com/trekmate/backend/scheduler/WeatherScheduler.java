package com.trekmate.backend.scheduler;

import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.service.WeatherService;
import com.trekmate.backend.service.impl.WeatherServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;


/**
 * WeatherScheduler — Tác vụ nền chạy mỗi ngày lúc 00:00 (nửa đêm).
 *
 * Quy trình:
 *  1. Lấy tất cả TourDeparture có departureDate trong vòng 16 ngày tiếp theo
 *     (Open-Meteo chỉ hỗ trợ forecast tối đa 16 ngày)
 *  2. Với mỗi departure, gọi Open-Meteo và lưu dữ liệu vào bảng departure_weather_daily
 *  3. Frontend/API sẽ đọc thẳng từ DB, không gọi Open-Meteo trực tiếp nữa
 *
 * Cron: "0 0 0 * * *" = 00:00:00 mỗi ngày
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherScheduler {

    private final TourDepartureRepository departureRepository;
    private final WeatherService weatherService;

    /**
     * Chạy mỗi ngày lúc 00:00 — cập nhật dự báo thời tiết cho tất cả departure
     * trong vòng 16 ngày tiếp theo.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateDailyWeather() {
        LocalDate today   = LocalDate.now();
        LocalDate maxDate = today.plusDays(WeatherServiceImpl.MAX_FORECAST_DAYS);

        log.info("[WeatherScheduler] Starting daily weather update | window: {} → {}", today, maxDate);

        List<TourDeparture> departures =
                departureRepository.findDeparturesForWeatherUpdate(today, maxDate);

        if (departures.isEmpty()) {
            log.info("[WeatherScheduler] No upcoming departures found in the forecast window.");
            return;
        }

        log.info("[WeatherScheduler] Found {} departures to update", departures.size());

        int success = 0;
        int failed  = 0;

        for (TourDeparture departure : departures) {
            try {
                weatherService.fetchAndSaveWeatherForDeparture(departure);
                success++;
                // Pause nhỏ để tránh rate-limit Open-Meteo (miễn phí, ~600 req/min)
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[WeatherScheduler] Failed for departure {}: {}", departure.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("[WeatherScheduler] Completed: {} success, {} failed out of {} departures",
                success, failed, departures.size());
    }

    /**
     * Chạy một lần ngay khi server khởi động (delay 60s để chờ context sẵn sàng).
     * Đảm bảo dữ liệu thời tiết luôn có sẵn ngay sau restart.
     */
    @Scheduled(initialDelay = 60_000, fixedDelay = Long.MAX_VALUE)
    public void initialWeatherLoad() {
        log.info("[WeatherScheduler] Running initial weather load after server startup...");
        updateDailyWeather();
    }
}
