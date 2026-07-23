package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.AiChatRequest;
import com.trekmate.backend.dto.response.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final GeminiClient geminiClient;
    private final AiContextBuilder contextBuilder;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Bạn là AI Trekking Assistant của hệ thống TrekMate — nền tảng đặt tour trekking hàng đầu Việt Nam.

            NHIỆM VỤ:
            - Tư vấn tour trekking phù hợp dựa trên thể lực, kinh nghiệm và sở thích của khách
            - Cung cấp thông tin thời tiết dự báo tại điểm đến
            - Gợi ý danh sách đồ cần mang (packing list)
            - Tư vấn an toàn trên trail

            QUY TẮC QUAN TRỌNG:
            - Trả lời bằng tiếng Việt, thân thiện, rõ ràng, định dạng Markdown (in đậm, danh sách, kèm link khi giới thiệu tour).
            - CHỈ SỬ DỤNG thông tin từ DỮ LIỆU TOUR HIỆN CÓ bên dưới (bao gồm tên tour, giá tour, độ khó, thời tiết và link tour chi tiết). Nếu không có thông tin, hãy nói "Hiện tại TrekMate chưa có thông tin này".
            - Khi gợi ý tour hoặc gửi link website cho khách hàng, hãy sử dụng đường dẫn trang web chính thức của TrekMate là: https://trek-mate-fe.vercel.app (ví dụ link chi tiết tour: https://trek-mate-fe.vercel.app/tours/{slug}). TUYỆT ĐỐI KHÔNG sử dụng trekmate.vn hoặc bất kỳ tên miền nào khác.
            - Khi tư vấn tour, hãy nêu rõ giá tiền (nếu có trong dữ liệu) để khách dễ cân nhắc ngân sách.
            - Luôn gợi ý khách hàng xem thêm trên website.
            %s
            """;

    public AiChatResponse processChat(AiChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        log.info("Processing chat for session: {}", sessionId);

        // 1. Lấy context dữ liệu thực từ hệ thống
        String dynamicContext = contextBuilder.buildSystemContext();
        String finalSystemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, dynamicContext);

        // 2. Gọi Gemini API
        String answer = geminiClient.generateContent(finalSystemPrompt, request.getMessage());

        // 3. Xây dựng quick replies (suggestions) dựa trên ngữ cảnh
        List<String> suggestions = generateSuggestions(request.getMessage(), answer);

        return AiChatResponse.builder()
                .answer(answer)
                .sessionId(sessionId)
                .suggestions(suggestions)
                .build();
    }

    private List<String> generateSuggestions(String userMessage, String aiAnswer) {
        // Có thể dùng logic phân tích đơn giản để gợi ý
        String lowerMsg = userMessage.toLowerCase();

        if (lowerMsg.contains("thời tiết")) {
            return Arrays.asList("Đồ cần mang theo?", "Nên chọn tour nào?", "Khó khăn gì cần lưu ý?");
        } else if (lowerMsg.contains("đồ") || lowerMsg.contains("mang")) {
            return Arrays.asList("Thuê đồ ở đâu?", "Thời tiết thế nào?", "Xem danh sách tour");
        } else if (lowerMsg.contains("chọn") || lowerMsg.contains("tour nào")) {
            return Arrays.asList("Tour dễ cho người mới?", "Cần chuẩn bị gì?", "Thời tiết tuần sau?");
        }

        // Mặc định
        return Arrays.asList("Tư vấn tour cho người mới", "Gợi ý đồ cần mang", "Thời tiết tuần sau");
    }
}
