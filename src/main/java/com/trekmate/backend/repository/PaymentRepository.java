package com.trekmate.backend.repository;

import com.trekmate.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByGatewayTxnId(String gatewayTxnId);

    Optional<Payment> findFirstByBookingIdAndStatusOrderByCreatedAtDesc(Long bookingId, String status);

    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
