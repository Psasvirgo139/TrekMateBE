package com.trekmate.backend.repository;

import com.trekmate.backend.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("SELECT l FROM Location l WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Location> searchByName(@Param("search") String search);

    boolean existsByNameIgnoreCase(String name);
}
