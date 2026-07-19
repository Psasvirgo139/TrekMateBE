package com.trekmate.backend.service;

import com.trekmate.backend.dto.response.WeatherDayResponse;
import com.trekmate.backend.model.TourDeparture;

import java.util.List;
import java.util.UUID;

/**
 * Service xử lý dữ liệu thời tiết cho tour departures.
 * Dữ liệu được cập nhật mỗi ngày lúc nửa đêm bởi WeatherScheduler,
 * và chỉ đọc từ DB khi user truy cập (không call API trực tiếp khi render).
 */
public interface WeatherService {

    /**
     * Lấy dự báo thời tiết từ DB cho departure.
     * Chỉ đọc từ database, KHÔNG gọi Open-Meteo trực tiếp.
     *
     * @param departureId UUID của TourDeparture
     * @return List dự báo thời tiết từng ngày (rỗng nếu chưa có dữ liệu)
     */
    List<WeatherDayResponse> getWeatherForDeparture(UUID departureId);

    /**
     * Fetch dữ liệu thời tiết từ Open-Meteo và lưu vào DB cho một departure cụ thể.
     * Dùng cho WeatherScheduler và admin refresh.
     *
     * @param departure TourDeparture entity (đã load kèm tour)
     */
    void fetchAndSaveWeatherForDeparture(TourDeparture departure);

    /**
     * Force-refresh dữ liệu thời tiết cho một departure (admin endpoint).
     *
     * @param departureId UUID của TourDeparture
     * @return List dự báo sau refresh
     */
    List<WeatherDayResponse> refreshWeatherForDeparture(UUID departureId);
}
