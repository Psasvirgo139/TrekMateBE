package com.trekmate.backend.repository;
import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID> {
    Optional<Tour> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Tour> findByStatus(TourStatus status, Pageable pageable);

    @Query("SELECT t FROM Tour t WHERE " +
           "(CAST(:search AS string) IS NULL OR LOWER(t.title) LIKE CAST(:search AS string) OR LOWER(t.shortDescription) LIKE CAST(:search AS string)) " +
           "AND (:difficulty IS NULL OR t.difficulty = :difficulty) " +
           "AND (:status IS NULL OR t.status = :status)")
    Page<Tour> findWithFilters(@Param("search") String search,
                               @Param("difficulty") DifficultyLevel difficulty,
                               @Param("status") TourStatus status,
                               Pageable pageable);

    @Query("SELECT t FROM Tour t WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.startLocation) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.endLocation) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:difficulty IS NULL OR t.difficulty = :difficulty) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:minDuration IS NULL OR t.durationDays >= :minDuration) " +
           "AND (:maxDuration IS NULL OR t.durationDays <= :maxDuration)")
    Page<Tour> findToursWithFilters(
            @Param("search") String search,
            @Param("difficulty") DifficultyLevel difficulty,
            @Param("status") TourStatus status,
            @Param("minDuration") Short minDuration,
            @Param("maxDuration") Short maxDuration,
            Pageable pageable
    );
}

