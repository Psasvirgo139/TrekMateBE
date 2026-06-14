package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingHistoryResponse {
    private Long id;
    private String bookingCode;
    private String tourTitle;
    private String tourSlug;
    private LocalDate departureDate;
    private Short durationDays;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private Short numParticipants;
    private LocalDateTime bookedAt;
}
