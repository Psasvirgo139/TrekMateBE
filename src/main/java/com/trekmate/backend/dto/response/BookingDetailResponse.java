package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDetailResponse {
    private Long id;
    private String bookingCode;
    private String tourTitle;
    private String tourSlug;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private Short durationDays;
    private Short durationNights;
    private BigDecimal totalPrice;
    private BigDecimal subtotalTour;
    private BigDecimal subtotalEquipment;
    private BigDecimal discountAmount;
    private BookingStatus status;
    private Short numParticipants;
    private List<Map<String, Object>> participantsInfo;
    private String meetingPoint;
    private String specialRequests;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime paidAt;
    private LocalDateTime bookedAt;

    // ── Departure UUID (dùng để gọi weather/AI API từ frontend) ──────────────
    private java.util.UUID departureId;

    // ── Dự báo thời tiết từ Open-Meteo ────────────────────────────────────────
    private List<WeatherDayResponse> weatherForecast;
    private String weatherOverallSummary;
    private String weatherIcon;
    private Short tempMinC;
    private Short tempMaxC;

    private List<RentalDetail> rentals;
    private List<PaymentDetail> payments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RentalDetail {
        private Long id;
        private String equipmentName;
        private String brand;
        private String model;
        private String imageUrl;
        private Short quantity;
        private Short rentalDays;
        private BigDecimal pricePerDay;
        private BigDecimal subtotal;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDetail {
        private Long id;
        private BigDecimal amount;
        private String method;
        private String gatewayTxnId;
        private String status;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
    }
}

