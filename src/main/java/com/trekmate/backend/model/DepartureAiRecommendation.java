package com.trekmate.backend.model;

import com.trekmate.backend.dto.response.AiGearRecommendationResponse;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Lưu kết quả gợi ý trang bị do AI (Gemini) tạo ra cho từng TourDeparture.
 *
 * Thiết kế dùng bảng riêng (departure_ai_recommendations) để tránh sửa đổi
 * bảng tour_departures đang được dùng rộng rãi trong toàn hệ thống.
 *
 * Dữ liệu được tính trước bởi AiRecommendationScheduler lúc 01:00 AM mỗi ngày.
 * Khi khách truy cập trang đặt tour, hệ thống đọc thẳng từ bảng này thay vì
 * gọi Gemini API trực tiếp, giúp giảm latency và chi phí API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "departure_ai_recommendations", indexes = {
        @Index(name = "idx_ai_rec_departure", columnList = "departure_id")
})
@EntityListeners(AuditingEntityListener.class)
public class DepartureAiRecommendation extends BaseEntity {

    /**
     * Tham chiếu 1-1 tới TourDeparture.
     * unique = true đảm bảo mỗi departure chỉ có 1 bản ghi AI recommendation.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false, unique = true)
    private TourDeparture departure;

    /**
     * Toàn bộ response từ Gemini AI, lưu dưới dạng JSONB.
     * Bao gồm: overallAdvice, essentials[], recommended[], disclaimer, weatherSummary, tourTitle.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation", columnDefinition = "jsonb", nullable = false)
    private AiGearRecommendationResponse recommendation;
}
