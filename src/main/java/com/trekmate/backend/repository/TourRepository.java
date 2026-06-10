package com.trekmate.backend.repository;
import com.trekmate.backend.model.Tour;
import com.trekmate.backend.model.enums.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface TourRepository extends JpaRepository<Tour, UUID> {
    Optional<Tour> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Tour> findByStatus(TourStatus status, Pageable pageable);
}
