package com.trekmate.backend.repository;

import com.trekmate.backend.model.TourAttribute;
import com.trekmate.backend.model.enums.TourAttributeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourAttributeRepository extends JpaRepository<TourAttribute, Long> {

    @Query("SELECT ta FROM TourAttribute ta WHERE ta.type = :type AND " +
           "(:search IS NULL OR :search = '' OR LOWER(ta.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<TourAttribute> searchByTypeAndContent(
            @Param("type") TourAttributeType type,
            @Param("search") String search
    );

    Optional<TourAttribute> findByTypeAndContentIgnoreCase(TourAttributeType type, String content);

    boolean existsByTypeAndContentIgnoreCase(TourAttributeType type, String content);
}
