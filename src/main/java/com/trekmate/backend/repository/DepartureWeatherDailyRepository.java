package com.trekmate.backend.repository;
import com.trekmate.backend.model.DepartureWeatherDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface DepartureWeatherDailyRepository extends JpaRepository<DepartureWeatherDaily, Long> {
    List<DepartureWeatherDaily> findByDepartureIdOrderByDayNumberAsc(UUID departureId);
    Optional<DepartureWeatherDaily> findByDepartureIdAndDayNumber(UUID departureId, Short dayNumber);
    boolean existsByDepartureId(UUID departureId);

    @Modifying
    @Query("DELETE FROM DepartureWeatherDaily w WHERE w.departure.id = :departureId")
    void deleteByDepartureId(@Param("departureId") UUID departureId);
}
