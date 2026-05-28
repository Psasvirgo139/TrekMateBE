package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.EquipmentCondition;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "equipment_rentals", indexes = {
        @Index(name = "idx_rentals_booking",   columnList = "booking_id"),
        @Index(name = "idx_rentals_equipment", columnList = "equipment_id")
})
public class EquipmentRental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Short quantity = 1;

    @Column(name = "rental_days", nullable = false)
    private Short rentalDays;

    @Column(name = "price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "returned_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_condition", length = 20)
    private EquipmentCondition returnCondition;

    @Column(name = "damage_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal damageFee = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}



