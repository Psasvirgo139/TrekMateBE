package com.trekmate.backend.repository;
import com.trekmate.backend.model.EquipmentRental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface EquipmentRentalRepository extends JpaRepository<EquipmentRental, Long> {
    List<EquipmentRental> findByBookingId(Long bookingId);
}

