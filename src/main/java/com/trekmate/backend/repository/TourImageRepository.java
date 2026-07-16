package com.trekmate.backend.repository;
import com.trekmate.backend.model.TourImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface TourImageRepository extends JpaRepository<TourImage, Long> {
    List<TourImage> findByTourIdOrderBySortOrderAsc(UUID tourId);

    /** Trả về URL ảnh bìa (isCover=true) của một tour, sắp xếp theo sortOrder để lấy cái đầu tiên nếu có nhiều bìa */
    @Query("SELECT img.imageUrl FROM TourImage img WHERE img.tour.id = :tourId AND img.isCover = true ORDER BY img.sortOrder ASC")
    List<String> findCoverUrlsByTourId(@Param("tourId") UUID tourId);

    /** Trả về URL ảnh đầu tiên (bất kỳ) của một tour — dùng làm fallback */
    @Query("SELECT img.imageUrl FROM TourImage img WHERE img.tour.id = :tourId ORDER BY img.sortOrder ASC")
    List<String> findFirstImageUrlByTourId(@Param("tourId") UUID tourId);
}

