package com.trekmate.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast" +
            "?latitude={lat}&longitude={lng}" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max" +
            "&timezone=Asia/Ho_Chi_Minh&forecast_days={days}";

    /**
     * Lay du bao thoi tiet tu Open-Meteo API theo toa do GPS.
     * 
     * @param lat Toa do vi do
     * @param lng Toa do kinh do
     * @param days So ngay du bao (toi da 16)
     * @return Chuoi string chua thong tin thoi tiet doc duoc (co the dua vao prompt Gemini)
     */
    public String getForecastSummary(BigDecimal lat, BigDecimal lng, int days) {
        if (lat == null || lng == null) {
            return "Không có dữ liệu tọa độ để lấy dự báo thời tiết.";
        }

        try {
            String url = WEATHER_API_URL.replace("{lat}", lat.toString())
                                        .replace("{lng}", lng.toString())
                                        .replace("{days}", String.valueOf(days));

            log.debug("Calling Weather API: {}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode daily = root.path("daily");

            if (daily.isMissingNode()) {
                return "Không có dữ liệu dự báo cho khu vực này.";
            }

            JsonNode time = daily.path("time");
            JsonNode tempMax = daily.path("temperature_2m_max");
            JsonNode tempMin = daily.path("temperature_2m_min");
            JsonNode precip = daily.path("precipitation_sum");
            JsonNode wind = daily.path("wind_speed_10m_max");

            StringBuilder summary = new StringBuilder();
            summary.append(String.format("Dự báo thời tiết tại tọa độ (%.4f, %.4f):\n", lat.doubleValue(), lng.doubleValue()));

            for (int i = 0; i < time.size(); i++) {
                summary.append("- Ngày ").append(time.get(i).asText()).append(": ");
                summary.append("Nhiệt độ ").append(tempMin.get(i).asDouble()).append(" - ").append(tempMax.get(i).asDouble()).append("°C, ");
                summary.append("Mưa ").append(precip.get(i).asDouble()).append("mm, ");
                summary.append("Gió ").append(wind.get(i).asDouble()).append("km/h.\n");
            }

            return summary.toString();

        } catch (Exception e) {
            log.error("Error fetching weather data: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu thời tiết.";
        }
    }
}
