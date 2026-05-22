package com.trekmate.backend.repository;
import com.trekmate.backend.model.Booking;
import com.trekmate.backend.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByBookingCode(String bookingCode);
    Page<Booking> findByUserId(UUID userId, Pageable pageable);
    Page<Booking> findByDepartureId(UUID departureId, Pageable pageable);
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
    @Query("SELECT COALESCE(SUM(b.numParticipants),0) FROM Booking b " +
           "WHERE b.departure.id = :depId AND b.status IN ('PENDING','CONFIRMED')")
    Integer sumBookedParticipants(@Param("depId") UUID departureId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'COMPLETED'")
    long countCompleted();
}
