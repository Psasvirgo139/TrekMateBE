package com.trekmate.backend.repository;

import com.trekmate.backend.model.DepartureAiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho bảng departure_ai_recommendations.
 * Dùng để tra cứu và lưu kết quả AI recommendation theo departure.
 */
@Repository
public interface DepartureAiRecommendationRepository extends JpaRepository<DepartureAiRecommendation, UUID> {

    /**
     * Tìm recommendation đã lưu theo departure ID.
     * Trả về Optional.empty() nếu chưa có (chưa được tính trước hoặc departure mới tạo).
     */
    Optional<DepartureAiRecommendation> findByDepartureId(UUID departureId);

    /**
     * Kiểm tra xem departure đã có AI recommendation chưa.
     */
    boolean existsByDepartureId(UUID departureId);
}
