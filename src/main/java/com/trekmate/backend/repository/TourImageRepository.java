package com.trekmate.backend.repository;
import com.trekmate.backend.model.TourImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface TourImageRepository extends JpaRepository<TourImage, UUID> {
    List<TourImage> findByTourIdOrderBySortOrderAsc(UUID tourId);
}
