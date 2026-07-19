package com.trekmate.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trekmate.backend.dto.response.AiGearRecommendationResponse;
import com.trekmate.backend.dto.response.AiGearRecommendationResponse.GearItem;
import com.trekmate.backend.dto.response.WeatherDayResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Equipment;
import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.repository.EquipmentRepository;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.service.AiRecommendationService;
import com.trekmate.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation gọi Gemini API để gợi ý trang bị trekking.
 * Sau khi nhận response, đối chiếu với danh sách equipment có sẵn để highlight.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationServiceImpl implements AiRecommendationService {

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${app.gemini.model}")
    private String geminiModel;

    private final TourDepartureRepository departureRepository;
    private final EquipmentRepository equipmentRepository;
    private final WeatherService weatherService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AiGearRecommendationResponse getGearRecommendation(UUID departureId) {
        // 1. Lấy thông tin departure
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found: " + departureId));

        Tour tour = departure.getTour();

        // 2. Lấy weather forecast
        List<WeatherDayResponse> weatherList = weatherService.getWeatherForDeparture(departureId);

        // 3. Lấy danh sách thiết bị có sẵn cho thuê
        List<Equipment> availableEquipment = equipmentRepository
                .findByIsActive(true, PageRequest.of(0, 100)).getContent();

        // 4. Build prompt (include equipment catalog so AI uses exact names)
        String prompt = buildGeminiPrompt(tour, departure, weatherList, availableEquipment);

        // 5. Call Gemini API
        String rawAiResponse = callGeminiApi(prompt);

        // 6. Parse response và đối chiếu với available equipment
        return parseAndEnrichResponse(rawAiResponse, tour, weatherList, availableEquipment);
    }

    // ─── Prompt builder ───────────────────────────────────────────────────────

    private String buildGeminiPrompt(Tour tour, TourDeparture departure, List<WeatherDayResponse> weather, List<Equipment> availableEquipment) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bạn là chuyên gia trekking Việt Nam. Hãy gợi ý danh sách trang bị cho chuyến đi sau:\n\n");

        // Tour info
        sb.append("## THÔNG TIN CHUYẾN ĐI\n");
        sb.append("- Tên tour: ").append(tour.getTitle()).append("\n");
        sb.append("- Thời gian: ").append(tour.getDurationDays()).append(" ngày ").append(tour.getDurationNights()).append(" đêm\n");
        if (tour.getMaxElevationM() != null) {
            sb.append("- Độ cao tối đa: ").append(tour.getMaxElevationM()).append(" m\n");
        }
        if (tour.getDifficulty() != null) {
            sb.append("- Độ khó: ").append(tour.getDifficulty().name()).append("\n");
        }
        if (tour.getDistanceKm() != null) {
            sb.append("- Quãng đường: ").append(tour.getDistanceKm()).append(" km\n");
        }
        sb.append("- Ngày khởi hành: ").append(departure.getDepartureDate()).append("\n");
        sb.append("- Ngày về: ").append(departure.getReturnDate()).append("\n\n");

        // Weather info
        if (!weather.isEmpty()) {
            sb.append("## DỰ BÁO THỜI TIẾT TỪNG NGÀY\n");
            for (WeatherDayResponse w : weather) {
                sb.append("Ngày ").append(w.dayNumber()).append(" (").append(w.forecastDate()).append("): ");
                sb.append(w.weatherSummary());
                if (w.tempMinC() != null && w.tempMaxC() != null) {
                    sb.append(", ").append(w.tempMinC()).append("°C – ").append(w.tempMaxC()).append("°C");
                }
                if (w.precipitationProb() != null && w.precipitationProb() > 20) {
                    sb.append(", xác suất mưa ").append(w.precipitationProb()).append("%");
                }
                if (w.windSpeedKmh() != null && w.windSpeedKmh() > 20) {
                    sb.append(", gió ").append(w.windSpeedKmh()).append(" km/h");
                }
                if (w.weatherWarning() != null) {
                    sb.append(" ⚠️ ").append(w.weatherWarning());
                }
                sb.append("\n");
            }
            sb.append("\n");
        } else {
            sb.append("## THỜI TIẾT\nChưa có dữ liệu dự báo thời tiết cụ thể.\n\n");
        }

        // ── QUAN TRỌNG: Danh sách thiết bị có thể thuê trong hệ thống ──────────
        if (!availableEquipment.isEmpty()) {
            sb.append("## DANH SÁCH THIẾT BỊ CÓ THỂ THUÊ TẠI TREKMATE\n");
            sb.append("(Đây là tên CHÍNH XÁC của các thiết bị có sẵn để cho thuê. ");
            sb.append("Khi đề xuất trang bị phù hợp với các thiết bị này, hãy dùng ĐÚNG TÊN bên dưới.)\n");
            for (Equipment eq : availableEquipment) {
                sb.append("- ").append(eq.getName());
                if (eq.getBrand() != null && !eq.getBrand().isBlank()) {
                    sb.append(" (").append(eq.getBrand()).append(")");
                }
                if (eq.getPricePerDay() != null) {
                    sb.append(" — ").append(formatPrice(eq.getPricePerDay())).append("/ngày");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Output format
        sb.append("## YÊU CẦU OUTPUT\n");
        sb.append("Trả về JSON hợp lệ theo cấu trúc sau (KHÔNG có markdown, KHÔNG có ```json):\n");
        sb.append("{\n");
        sb.append("  \"overallAdvice\": \"Lời khuyên tổng quát ngắn gọn cho chuyến đi\",\n");
        sb.append("  \"essentials\": [\n");
        sb.append("    {\"name\": \"Tên trang bị\", \"reason\": \"Lý do cần thiết\", \"category\": \"Phân loại (vd: Bảo hộ, Quần áo, Cắm trại, Kỹ thuật, Y tế, Ăn uống)\"}\n");
        sb.append("  ],\n");
        sb.append("  \"recommended\": [\n");
        sb.append("    {\"name\": \"Tên trang bị\", \"reason\": \"Lý do nên mang\", \"category\": \"Phân loại\"}\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("Quy tắc:\n");
        sb.append("- 'essentials': 5-8 món không thể thiếu cho điều kiện thời tiết và địa hình này\n");
        sb.append("- 'recommended': 4-6 món nên mang thêm nếu có\n");
        sb.append("- NẾU thiết bị có tên CHÍNH XÁC trong danh sách cho thuê ở trên, hãy dùng ĐÚNG TÊN đó\n");
        sb.append("- Với thiết bị không có trong danh sách cho thuê, đặt tên cụ thể, thực tế bằng tiếng Việt\n");
        sb.append("- CHỈ trả về JSON thuần, không có text thừa\n");

        return sb.toString();
    }


    // ─── Gemini API call ──────────────────────────────────────────────────────

    private String callGeminiApi(String prompt) {
        String url = geminiBaseUrl + "/models/" + geminiModel + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 1024,
                        "responseMimeType", "application/json"
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0)
                       .path("content").path("parts").get(0)
                       .path("text").asText();
        } catch (Exception e) {
            log.error("[AI] Gemini API call failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "AI service unavailable: " + e.getMessage());
        }
    }

    // ─── Response parser + enrichment ────────────────────────────────────────

    private AiGearRecommendationResponse parseAndEnrichResponse(
            String rawJson, Tour tour,
            List<WeatherDayResponse> weatherList,
            List<Equipment> availableEquipment) {

        String overallAdvice = "Chuẩn bị đầy đủ trang bị phù hợp với điều kiện thời tiết.";
        List<GearItem> essentials = new ArrayList<>();
        List<GearItem> recommended = new ArrayList<>();

        try {
            // Clean up JSON nếu Gemini trả về có markdown code fence
            String cleanJson = rawJson.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
            }

            JsonNode root = objectMapper.readTree(cleanJson);
            overallAdvice = root.path("overallAdvice").asText(overallAdvice);

            JsonNode essentialsNode = root.path("essentials");
            if (essentialsNode.isArray()) {
                for (JsonNode item : essentialsNode) {
                    essentials.add(buildGearItem(item, availableEquipment));
                }
            }

            JsonNode recommendedNode = root.path("recommended");
            if (recommendedNode.isArray()) {
                for (JsonNode item : recommendedNode) {
                    recommended.add(buildGearItem(item, availableEquipment));
                }
            }

        } catch (Exception e) {
            log.error("[AI] Failed to parse Gemini response: {}", e.getMessage());
            log.debug("[AI] Raw response: {}", rawJson);
        }

        // Build weather summary text
        String weatherSummary = buildWeatherSummary(weatherList);

        return new AiGearRecommendationResponse(
                tour.getTitle(),
                weatherSummary,
                overallAdvice,
                essentials,
                recommended,
                "Gợi ý được tạo bởi Gemini AI. Hãy tham khảo hướng dẫn viên trước khi khởi hành."
        );
    }

    private GearItem buildGearItem(JsonNode itemNode, List<Equipment> availableEquipment) {
        String name = itemNode.path("name").asText("");
        String reason = itemNode.path("reason").asText("");
        String category = itemNode.path("category").asText("");

        // Tìm equipment khớp trong kho cho thuê (fuzzy match theo tên)
        Optional<Equipment> matched = findMatchingEquipment(name, availableEquipment);

        if (matched.isPresent()) {
            Equipment eq = matched.get();
            String price = eq.getPricePerDay() != null
                    ? formatPrice(eq.getPricePerDay()) + "/ngày"
                    : null;
            return new GearItem(name, reason, category, true, eq.getId(), eq.getImageUrl(), price);
        }

        return new GearItem(name, reason, category, false, null, null, null);
    }

    /**
     * Fuzzy match: tìm equipment có tên chứa từ khoá chung nhất của gear name.
     * Chiến lược: normalize cả 2 về lowercase, tách từ, đếm từ chung.
     */
    private Optional<Equipment> findMatchingEquipment(String gearName, List<Equipment> equipmentList) {
        if (gearName == null || gearName.isBlank()) return Optional.empty();

        String normalized = normalize(gearName);
        Set<String> gearTokens = tokenize(normalized);

        Equipment bestMatch = null;
        int bestScore = 0;

        for (Equipment eq : equipmentList) {
            if (!Boolean.TRUE.equals(eq.getIsActive())) continue;
            String eqNorm = normalize(eq.getName());
            Set<String> eqTokens = tokenize(eqNorm);

            // Intersection
            Set<String> common = new HashSet<>(gearTokens);
            common.retainAll(eqTokens);

            int score = common.size();
            // Bonus: direct contains
            if (eqNorm.contains(normalized) || normalized.contains(eqNorm)) {
                score += 3;
            }

            if (score > bestScore && score >= 1) {
                bestScore = score;
                bestMatch = eq;
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9 ]", " ")
                .trim();
    }

    private Set<String> tokenize(String s) {
        return Arrays.stream(s.split("\\s+"))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toSet());
    }


    private String buildWeatherSummary(List<WeatherDayResponse> weatherList) {
        if (weatherList.isEmpty()) return "Chưa có dữ liệu thời tiết.";
        OptionalDouble avgMax = weatherList.stream()
                .filter(w -> w.tempMaxC() != null)
                .mapToDouble(WeatherDayResponse::tempMaxC).average();
        OptionalDouble avgMin = weatherList.stream()
                .filter(w -> w.tempMinC() != null)
                .mapToDouble(WeatherDayResponse::tempMinC).average();
        long rainyDays = weatherList.stream()
                .filter(w -> w.precipitationProb() != null && w.precipitationProb() > 40).count();

        StringBuilder sb = new StringBuilder();
        if (avgMin.isPresent() && avgMax.isPresent()) {
            sb.append(String.format("Nhiệt độ %.0f–%.0f°C", avgMin.getAsDouble(), avgMax.getAsDouble()));
        }
        if (rainyDays > 0) {
            sb.append(", dự kiến ").append(rainyDays).append(" ngày có mưa");
        }
        return sb.toString();
    }

    private String formatPrice(BigDecimal price) {
        return String.format("%,.0f₫", price.doubleValue());
    }
}
