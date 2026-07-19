package com.trekmate.backend.controller;

import com.trekmate.backend.dto.response.AiGearRecommendationResponse;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.WeatherDayResponse;
import com.trekmate.backend.service.AiRecommendationService;
import com.trekmate.backend.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller cung cấp API dự báo thời tiết và gợi ý trang bị AI cho tour departures.
 */
@RestController
@RequestMapping("/v1/weather")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Weather & AI", description = "Dự báo thời tiết (Open-Meteo) và gợi ý trang bị AI (Gemini) cho chuyến trekking")
public class WeatherController {

    private final WeatherService weatherService;
    private final AiRecommendationService aiRecommendationService;

    /**
     * Lấy dự báo thời tiết theo ngày cho một departure.
     * Trả về từ cache DB nếu còn mới (< 6h), ngược lại fetch mới từ Open-Meteo.
     */
    @GetMapping("/departure/{departureId}")
    @Operation(summary = "Lấy dự báo thời tiết cho departure",
               description = "Đọc từ DB (cập nhật mỗi đêm bởi scheduler). Nếu DB chưa có dữ liệu, tự động fetch từ Open-Meteo.")
    public ApiResponse<List<WeatherDayResponse>> getWeatherForecast(@PathVariable UUID departureId) {
        log.info("[Weather] GET forecast for departure: {}", departureId);
        List<WeatherDayResponse> data = weatherService.getWeatherForDeparture(departureId);

        // Nếu DB chưa có dữ liệu (scheduler chưa chạy hoặc departure mới tạo),
        // trigger on-demand refresh từ Open-Meteo và cache lại cho lần sau.
        if (data.isEmpty()) {
            log.info("[Weather] DB empty — triggering on-demand fetch for departure: {}", departureId);
            data = weatherService.refreshWeatherForDeparture(departureId);
        }

        return ApiResponse.<List<WeatherDayResponse>>builder()
                .code(200)
                .message(data.isEmpty()
                        ? "Chưa có dữ liệu thời tiết — chuyến đi ngoài phạm vi 16 ngày dự báo"
                        : "Lấy dự báo thời tiết thành công")
                .data(data)
                .build();
    }


    /**
     * Force-refresh dữ liệu thời tiết từ Open-Meteo (dành cho admin).
     */
    @PostMapping("/departure/{departureId}/refresh")
    @Operation(summary = "[Admin] Refresh dữ liệu thời tiết",
               description = "Xóa cache cũ và fetch lại từ Open-Meteo.")
    public ApiResponse<List<WeatherDayResponse>> refreshWeather(@PathVariable UUID departureId) {
        log.info("[Weather] REFRESH for departure: {}", departureId);
        List<WeatherDayResponse> data = weatherService.refreshWeatherForDeparture(departureId);
        return ApiResponse.<List<WeatherDayResponse>>builder()
                .code(200)
                .message("Làm mới dữ liệu thời tiết thành công")
                .data(data)
                .build();
    }

    /**
     * Gọi Gemini AI để gợi ý trang bị phù hợp với điều kiện tour và thời tiết.
     * Items có sẵn trong kho cho thuê sẽ được đánh dấu isAvailableForRent = true.
     */
    @GetMapping("/departure/{departureId}/ai-recommendation")
    @Operation(summary = "Gợi ý trang bị AI",
               description = "Dùng Gemini API phân tích tour + thời tiết, đề xuất danh sách trang bị. " +
                             "Items khớp với kho cho thuê sẽ được highlight để người dùng dễ thuê thêm.")
    public ApiResponse<AiGearRecommendationResponse> getAiRecommendation(@PathVariable UUID departureId) {
        log.info("[AI] GET gear recommendation for departure: {}", departureId);
        AiGearRecommendationResponse data = aiRecommendationService.getGearRecommendation(departureId);
        return ApiResponse.<AiGearRecommendationResponse>builder()
                .code(200)
                .message("Gợi ý trang bị AI thành công")
                .data(data)
                .build();
    }
}
