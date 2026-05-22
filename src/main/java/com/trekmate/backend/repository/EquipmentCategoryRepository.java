package com.trekmate.backend.repository;
import com.trekmate.backend.model.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, UUID> {
    Optional<EquipmentCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
