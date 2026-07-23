package com.trekmate.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trekmate.backend.dto.response.WeatherDayResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.DepartureWeatherDaily;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.model.enums.WarningLevel;
import com.trekmate.backend.repository.DepartureWeatherDailyRepository;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * WeatherServiceImpl — Kiến trúc DB-first:
 * - getWeatherForDeparture() → chỉ đọc từ DB (không gọi API)
 * - fetchAndSaveWeatherForDeparture() → gọi Open-Meteo + lưu DB (dùng cho
 * scheduler)
 * - refreshWeatherForDeparture() → force refresh (dùng cho admin)
 *
 * Open-Meteo hỗ trợ tối đa 16 ngày forecast. Scheduler chạy mỗi nửa đêm
 * sẽ cập nhật tất cả departure trong 16 ngày tiếp theo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    // Hà Nội fallback nếu tour không có tọa độ
    private static final double FALLBACK_LAT = 21.02;
    private static final double FALLBACK_LNG = 105.83;
    // Open-Meteo free tier: quảng cáo 16 ngày nhưng thực tế ~14 ngày.
    // Dùng 14 để an toàn, tránh 400 "start_date out of allowed range".
    public static final int MAX_FORECAST_DAYS = 14;

    private final TourDepartureRepository departureRepository;
    private final DepartureWeatherDailyRepository weatherRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast" +
            "?latitude={lat}&longitude={lng}" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max" +
            "&timezone=Asia/Ho_Chi_Minh&forecast_days={days}";

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Chỉ đọc từ DB. Không gọi Open-Meteo.
     * Dữ liệu được cập nhật mỗi đêm bởi WeatherScheduler.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WeatherDayResponse> getWeatherForDeparture(UUID departureId) {
        List<DepartureWeatherDaily> rows = weatherRepository.findByDepartureIdOrderByDayNumberAsc(departureId);
        log.debug("[Weather] DB lookup for departure {} → {} rows", departureId, rows.size());
        return rows.stream().map(this::toResponse).toList();
    }

    /**
     * Gọi Open-Meteo + lưu vào DB cho một departure.
     * Được gọi bởi WeatherScheduler mỗi nửa đêm.
     */
    @Override
    @Transactional
    public void fetchAndSaveWeatherForDeparture(TourDeparture departure) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = departure.getDepartureDate();
        LocalDate returnDate = departure.getReturnDate();
        LocalDate maxDate = today.plusDays(MAX_FORECAST_DAYS);

        // Bỏ qua nếu departure đã qua hoặc ngoài window 16 ngày
        if (startDate == null) {
            log.warn("[Weather] Departure {} has null departureDate — skipping", departure.getId());
            return;
        }
        if (startDate.isBefore(today)) {
            log.debug("[Weather] Departure {} is in the past ({}) — skipping", departure.getId(), startDate);
            return;
        }
        if (startDate.isAfter(maxDate)) {
            log.debug("[Weather] Departure {} starts on {} which is beyond {}-day window — skipping",
                    departure.getId(), startDate, MAX_FORECAST_DAYS);
            return;
        }

        // Giới hạn endDate trong window 16 ngày
        LocalDate endDate = (returnDate != null && returnDate.isBefore(maxDate)) ? returnDate : maxDate;
        if (endDate.isBefore(startDate)) {
            endDate = startDate; // ít nhất 1 ngày
        }

        // Tọa độ tour
        double lat = FALLBACK_LAT;
        double lng = FALLBACK_LNG;
        if (departure.getTour() != null) {
            if (departure.getTour().getStartLat() != null)
                lat = departure.getTour().getStartLat().doubleValue();
            if (departure.getTour().getStartLng() != null)
                lng = departure.getTour().getStartLng().doubleValue();
        }

        String url = buildOpenMeteoUrl(lat, lng, startDate, endDate);
        log.info("[Weather] Fetching for departure {} | {} → {} | lat={}, lng={}",
                departure.getId(), startDate, endDate, lat, lng);

        try {
            String rawJson = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode daily = root.path("daily");

            JsonNode dates = daily.path("time");
            JsonNode tempMax = daily.path("temperature_2m_max");
            JsonNode tempMin = daily.path("temperature_2m_min");
            JsonNode precipSum = daily.path("precipitation_sum");
            JsonNode precipProb = daily.path("precipitation_probability_max");
            JsonNode windMax = daily.path("wind_speed_10m_max");
            JsonNode windGust = daily.path("wind_gusts_10m_max");
            JsonNode uvIndex = daily.path("uv_index_max");
            JsonNode wmoCode = daily.path("weather_code");

            if (dates.size() == 0) {
                log.warn("[Weather] Open-Meteo returned 0 days for departure {}", departure.getId());
                return;
            }

            // Xóa dữ liệu cũ rồi insert lại
            weatherRepository.deleteByDepartureId(departure.getId());

            List<DepartureWeatherDaily> entries = new ArrayList<>();
            for (int i = 0; i < dates.size(); i++) {
                LocalDate forecastDate = LocalDate.parse(dates.get(i).asText());
                int code = wmoCode.get(i).asInt(0);
                double pcp = precipSum.get(i).asDouble(0);
                double wnd = windMax.get(i).asDouble(0);

                WarningLevel level = calcWarningLevel(pcp, wnd, code);
                String warningText = buildWarningText(pcp, wnd, code, level);

                entries.add(DepartureWeatherDaily.builder()
                        .departure(departure)
                        .dayNumber((short) (i + 1))
                        .forecastDate(forecastDate)
                        .weatherSummary(wmoCodeToSummary(code))
                        .weatherIcon(wmoCodeToIcon(code))
                        .tempMaxC(safeShort(tempMax.get(i)))
                        .tempMinC(safeShort(tempMin.get(i)))
                        .precipitationMm(safeBigDecimal(precipSum.get(i)))
                        .precipitationProb(safeShort(precipProb.get(i)))
                        .windSpeedKmh(safeShort(windMax.get(i)))
                        .windGustKmh(safeShort(windGust.get(i)))
                        .uvIndex(safeShort(uvIndex.get(i)))
                        .weatherWarning(warningText)
                        .warningLevel(level)
                        .dataSource("open-meteo")
                        .forecastUpdatedAt(LocalDateTime.now())
                        .isActual(false)
                        .build());

            }

            weatherRepository.saveAll(entries);
            log.info("[Weather] Saved {} records for departure {} ({} → {})",
                    entries.size(), departure.getId(), startDate, endDate);

        } catch (Exception e) {
            log.error("[Weather] Failed to fetch Open-Meteo for departure {}: {}", departure.getId(), e.getMessage(),
                    e);
        }
    }

    /**
     * Admin: force-refresh và trả về kết quả mới.
     */
    @Override
    @Transactional
    public List<WeatherDayResponse> refreshWeatherForDeparture(UUID departureId) {
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(
                        () -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found: " + departureId));
        fetchAndSaveWeatherForDeparture(departure);
        return getWeatherForDeparture(departureId);
    }

    /**
     * Lay du bao thoi tiet tu Open-Meteo API theo toa do GPS.
     * 
     * @param lat  Toa do vi do
     * @param lng  Toa do kinh do
     * @param days So ngay du bao (toi da 16)
     * @return Chuoi string chua thong tin thoi tiet doc duoc (co the dua vao prompt
     *         Gemini)
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
            summary.append(
                    String.format("Dự báo thời tiết tại tọa độ (%.4f, %.4f):\n", lat.doubleValue(), lng.doubleValue()));

            for (int i = 0; i < time.size(); i++) {
                summary.append("- Ngày ").append(time.get(i).asText()).append(": ");
                summary.append("Nhiệt độ ").append(tempMin.get(i).asDouble()).append(" - ")
                        .append(tempMax.get(i).asDouble()).append("°C, ");
                summary.append("Mưa ").append(precip.get(i).asDouble()).append("mm, ");
                summary.append("Gió ").append(wind.get(i).asDouble()).append("km/h.\n");
            }

            return summary.toString();

        } catch (Exception e) {
            log.error("Error fetching weather data: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu thời tiết.";
        }
    }
    // ─── URL builder ──────────────────────────────────────────────────────────

    private String buildOpenMeteoUrl(double lat, double lng, LocalDate start, LocalDate end) {
        // Chỉ dùng các biến daily hợp lệ của Open-Meteo API.
        // Lưu ý: relativehumidity_2m_max và cloudcover_mean KHÔNG phải daily variables.
        // wind_speed_10m_max, wind_gusts_10m_max, weather_code là tên mới từ phiên bản
        // API hiện tại.
        return "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + String.format("%.6f", lat)
                + "&longitude=" + String.format("%.6f", lng)
                + "&daily=temperature_2m_max,temperature_2m_min"
                + ",precipitation_sum,precipitation_probability_max"
                + ",wind_speed_10m_max,wind_gusts_10m_max"
                + ",uv_index_max,weather_code"
                + "&timezone=Asia/Ho_Chi_Minh"
                + "&start_date=" + start
                + "&end_date=" + end;
    }

    // ─── WMO Weather Code mapping ─────────────────────────────────────────────

    private String wmoCodeToSummary(int code) {
        if (code == 0)
            return "Trời quang";
        if (code == 1)
            return "Ít mây";
        if (code == 2)
            return "Mây rải rác";
        if (code == 3)
            return "Nhiều mây";
        if (code >= 45 && code <= 48)
            return "Sương mù";
        if (code >= 51 && code <= 55)
            return "Mưa phùn";
        if (code >= 61 && code <= 65)
            return "Mưa";
        if (code >= 71 && code <= 75)
            return "Tuyết";
        if (code >= 80 && code <= 82)
            return "Mưa rào";
        if (code >= 95 && code <= 99)
            return "Giông bão";
        return "Không xác định";
    }

    private String wmoCodeToIcon(int code) {
        if (code == 0)
            return "sunny";
        if (code == 1)
            return "mostly-sunny";
        if (code == 2)
            return "partly-cloudy";
        if (code == 3)
            return "cloudy";
        if (code >= 45 && code <= 48)
            return "foggy";
        if (code >= 51 && code <= 55)
            return "drizzle";
        if (code >= 61 && code <= 65)
            return "rainy";
        if (code >= 71 && code <= 75)
            return "snowy";
        if (code >= 80 && code <= 82)
            return "showers";
        if (code >= 95 && code <= 99)
            return "thunderstorm";
        return "cloudy";
    }

    private WarningLevel calcWarningLevel(double precipMm, double windKmh, int code) {
        if (code >= 95 || windKmh >= 75 || precipMm >= 50)
            return WarningLevel.DANGER;
        if (code >= 80 || windKmh >= 50 || precipMm >= 25)
            return WarningLevel.WARNING;
        if (code >= 61 || windKmh >= 30 || precipMm >= 10)
            return WarningLevel.CAUTION;
        return WarningLevel.INFO;
    }

    private String buildWarningText(double precipMm, double windKmh, int code, WarningLevel level) {
        if (level == WarningLevel.INFO)
            return null;
        List<String> parts = new ArrayList<>();
        if (code >= 95)
            parts.add("Giông bão nguy hiểm");
        else if (code >= 80)
            parts.add("Mưa rào mạnh");
        else if (code >= 61)
            parts.add("Mưa lớn");
        if (windKmh >= 75)
            parts.add("Gió rất mạnh (" + (int) windKmh + " km/h)");
        else if (windKmh >= 50)
            parts.add("Gió mạnh (" + (int) windKmh + " km/h)");
        if (precipMm >= 50)
            parts.add("Lượng mưa rất cao (" + precipMm + " mm)");
        else if (precipMm >= 25)
            parts.add("Mưa lớn (" + precipMm + " mm)");
        return parts.isEmpty() ? null : String.join(". ", parts);
    }

    // ─── Type helpers ─────────────────────────────────────────────────────────

    private Short safeShort(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        return (short) Math.round(node.asDouble(0));
    }

    private BigDecimal safeBigDecimal(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        return BigDecimal.valueOf(node.asDouble(0)).setScale(1, java.math.RoundingMode.HALF_UP);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private WeatherDayResponse toResponse(DepartureWeatherDaily w) {
        return new WeatherDayResponse(
                w.getDayNumber(),
                w.getForecastDate(),
                w.getLocationLabel(),
                w.getElevationM(),
                w.getWeatherSummary(),
                w.getWeatherIcon(),
                w.getTempMinC(),
                w.getTempMaxC(),
                w.getFeelsLikeMinC(),
                w.getFeelsLikeMaxC(),
                w.getPrecipitationMm(),
                w.getPrecipitationProb(),
                w.getWindSpeedKmh(),
                w.getWindGustKmh(),
                w.getHumidityPct(),
                w.getVisibilityKm(),
                w.getWeatherWarning(),
                w.getWarningLevel());
    }
}
