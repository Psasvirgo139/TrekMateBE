package com.trekmate.backend.model;

import com.trekmate.backend.model.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_user",      columnList = "user_id"),
        @Index(name = "idx_bookings_departure", columnList = "departure_id"),
        @Index(name = "idx_bookings_status",    columnList = "status"),
        @Index(name = "idx_bookings_code",      columnList = "booking_code", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "booking_code", nullable = false, unique = true, length = 20)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private TourDeparture departure;

    @Column(name = "num_participants", nullable = false)
    @Builder.Default
    private Short numParticipants = 1;

    // JSONB: [{"name":"...","dob":"...","phone":"...","emergency_contact":"..."}]
    @Column(name = "participants_info", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String participantsInfo = "[]";

    @Column(name = "price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "subtotal_tour", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalTour;

    @Column(name = "subtotal_equipment", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotalEquipment = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "is_join_tour", nullable = false)
    @Builder.Default
    private Boolean isJoinTour = false;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "paid_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @CreatedDate
    @Column(name = "booked_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP")
    private LocalDateTime bookedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    // ── Relationships ───────────────────────────────────────────────────────────

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Review review;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<EquipmentRental> rentals = new ArrayList<>();

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}

