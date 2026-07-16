package com.trekmate.backend.repository;

import com.trekmate.backend.model.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    Page<Equipment> findByCategoryIdAndIsActive(Long categoryId, Boolean isActive, Pageable pageable);
    Page<Equipment> findByIsActive(Boolean isActive, Pageable pageable);
    Page<Equipment> findByCategoryId(Long categoryId, Pageable pageable);
    boolean existsByCategoryId(Long categoryId);
    long countByIsActive(Boolean isActive);
}

