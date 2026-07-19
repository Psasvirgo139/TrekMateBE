package com.trekmate.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trekmate.backend.dto.response.AiGearRecommendationResponse;
import com.trekmate.backend.dto.response.AiGearRecommendationResponse.GearItem;
import com.trekmate.backend.dto.response.WeatherDayResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.DepartureAiRecommendation;
import com.trekmate.backend.model.Equipment;
import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.repository.DepartureAiRecommendationRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation gọi Gemini API để gợi ý trang bị trekking.
 *
 * Chiến lược cache DB-first:
 *  - getGearRecommendation() → đọc từ bảng departure_ai_recommendations trước.
 *    Nếu chưa có → gọi Gemini, lưu kết quả vào DB, trả về ngay.
 *  - precalculateAiRecommendations() → batch tính trước mỗi đêm lúc 01:00 AM
 *    cho các departure trong 10 ngày tiếp theo chưa có cached data.
 *
 * Phạm vi đặt vào @Transactional để đảm bảo Hibernate session còn mở khi
 * truy cập các quan hệ LAZY (departure.getTour().getTitle(), v.v.).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationServiceImpl implements AiRecommendationService {

    // ─── Số ngày scan cho batch pre-calculation ──────────────────────────────
    private static final int AI_SCAN_WINDOW_DAYS = 10;

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${app.gemini.model}")
    private String geminiModel;

    private final TourDepartureRepository departureRepository;
    private final DepartureAiRecommendationRepository aiRecommendationRepository;
    private final EquipmentRepository equipmentRepository;
    private final WeatherService weatherService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Lấy AI recommendation cho một departure.
     * Cache-first: đọc từ DB trước, chỉ gọi Gemini nếu chưa có cache.
     */
    @Override
    @Transactional
    public AiGearRecommendationResponse getGearRecommendation(UUID departureId) {
        // 1. Kiểm tra cache trước
        Optional<DepartureAiRecommendation> cached = aiRecommendationRepository.findByDepartureId(departureId);
        if (cached.isPresent()) {
            log.info("[AI] Cache HIT for departure: {}", departureId);
            return cached.get().getRecommendation();
        }

        log.info("[AI] Cache MISS for departure: {} — calling Gemini", departureId);

        // 2. Lấy thông tin departure (JOIN FETCH tour để tránh LazyInitializationException)
        TourDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found: " + departureId));

        // 3. Tính toán và lưu vào cache
        AiGearRecommendationResponse result = generateAndCache(departure);
        return result;
    }

    /**
     * Batch pre-calculation: quét tất cả departure trong 10 ngày tiếp theo
     * chưa có AI recommendation trong DB, rồi gọi Gemini và lưu lại.
     * Được gọi bởi AiRecommendationScheduler lúc 01:00 AM mỗi ngày.
     */
    @Override
    @Transactional
    public void precalculateAiRecommendations() {
        LocalDate today   = LocalDate.now();
        LocalDate maxDate = today.plusDays(AI_SCAN_WINDOW_DAYS);

        log.info("[AI-Scheduler] Scanning departures without AI cache | window: {} → {}", today, maxDate);

        List<TourDeparture> departures =
                departureRepository.findDeparturesForAiRecommendation(today, maxDate);

        if (departures.isEmpty()) {
            log.info("[AI-Scheduler] All departures in window already have AI recommendation cache.");
            return;
        }

        log.info("[AI-Scheduler] Found {} departures to process", departures.size());

        int success = 0, failed = 0;

        for (TourDeparture departure : departures) {
            try {
                generateAndCache(departure);
                success++;
                // Nhỏ delay để tránh rate-limit Gemini API
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[AI-Scheduler] Failed for departure {}: {}", departure.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("[AI-Scheduler] Completed: {} success, {} failed out of {} departures",
                success, failed, departures.size());
    }

    // ─── Core generation + cache logic ───────────────────────────────────────

    /**
     * Gọi Gemini để tạo recommendation cho một departure, sau đó lưu vào DB.
     * Session JPA phải còn mở (gọi từ trong @Transactional method).
     */
    private AiGearRecommendationResponse generateAndCache(TourDeparture departure) {
        Tour tour = departure.getTour();

        // Lấy weather forecast từ DB
        List<WeatherDayResponse> weatherList = weatherService.getWeatherForDeparture(departure.getId());

        // Lấy danh sách thiết bị cho thuê
        List<Equipment> availableEquipment = equipmentRepository
                .findByIsActive(true, PageRequest.of(0, 100)).getContent();

        // Build prompt + gọi Gemini
        String prompt     = buildGeminiPrompt(tour, departure, weatherList, availableEquipment);
        String rawResponse = callGeminiApi(prompt);

        // Parse + enrich với equipment info
        AiGearRecommendationResponse result = parseAndEnrichResponse(rawResponse, tour, weatherList, availableEquipment);

        // Lưu vào bảng departure_ai_recommendations
        DepartureAiRecommendation entity = DepartureAiRecommendation.builder()
                .departure(departure)
                .recommendation(result)
                .build();
        aiRecommendationRepository.save(entity);

        log.info("[AI] Cached recommendation for departure: {}", departure.getId());
        return result;
    }

    // ─── Prompt builder ───────────────────────────────────────────────────────

    private String buildGeminiPrompt(Tour tour, TourDeparture departure,
                                     List<WeatherDayResponse> weather,
                                     List<Equipment> availableEquipment) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bạn là chuyên gia trekking Việt Nam. Hãy gợi ý danh sách trang bị cho chuyến đi sau:\n\n");

        // Tour info
        sb.append("## THÔNG TIN CHUYẾN ĐI\n");
        sb.append("- Tên tour: ").append(tour.getTitle()).append("\n");
        sb.append("- Thời gian: ").append(tour.getDurationDays()).append(" ngày ")
          .append(tour.getDurationNights()).append(" đêm\n");
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

        // Danh sách thiết bị có thể thuê
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

        // Output format — compact để tiết kiệm input token, nhường chỗ cho response
        sb.append("## OUTPUT\n");
        sb.append("JSON thuần (không markdown). Schema:\n");
        sb.append("{\"overallAdvice\":\"string\",\"essentials\":[{\"name\":\"string\",\"reason\":\"string\",\"category\":\"string\"}],\"recommended\":[{\"name\":\"string\",\"reason\":\"string\",\"category\":\"string\"}]}\n\n");
        sb.append("- essentials: 5 món bắt buộc; recommended: 4 món nên có\n");
        sb.append("- Dùng đúng tên trong danh sách thuê nếu phù hợp\n");
        sb.append("- reason ngắn gọn (<= 12 từ); overallAdvice <= 25 từ\n");


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
                        "maxOutputTokens", 2048,
                        "responseMimeType", "application/json",
                        // Tắt thinking budget để toàn bộ token dành cho output JSON
                        // gemini-3.5-flash là thinking model — không set thinkingBudget
                        // sẽ tiêu tốn hàng trăm token cho thinking trước khi sinh output
                        "thinkingConfig", Map.of("thinkingBudget", 0)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode parts = root.path("candidates").get(0)
                                 .path("content").path("parts");
            // Nối tất cả parts text lại (model đôi khi split thành nhiều parts)
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : parts) {
                String text = part.path("text").asText("");
                if (!text.isBlank()) sb.append(text);
            }
            return sb.toString();
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
        List<GearItem> essentials   = new ArrayList<>();
        List<GearItem> recommended  = new ArrayList<>();

        try {
            // Dọn dẹp JSON nếu Gemini trả về có markdown code fence
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
        String name     = itemNode.path("name").asText("");
        String reason   = itemNode.path("reason").asText("");
        String category = itemNode.path("category").asText("");

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
     * Fuzzy match: tìm equipment có tên chứa từ khoá chung nhất.
     * Normalize về lowercase + bỏ dấu + tách từ, đếm số từ trùng.
     */
    private Optional<Equipment> findMatchingEquipment(String gearName, List<Equipment> equipmentList) {
        if (gearName == null || gearName.isBlank()) return Optional.empty();

        String          normalized = normalize(gearName);
        Set<String>     gearTokens = tokenize(normalized);

        Equipment bestMatch = null;
        int       bestScore = 0;

        for (Equipment eq : equipmentList) {
            if (!Boolean.TRUE.equals(eq.getIsActive())) continue;
            String      eqNorm   = normalize(eq.getName());
            Set<String> eqTokens = tokenize(eqNorm);

            Set<String> common = new HashSet<>(gearTokens);
            common.retainAll(eqTokens);

            int score = common.size();
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
