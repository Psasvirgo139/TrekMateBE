package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.CancelBookingRequest;
import com.trekmate.backend.dto.response.BookingDetailResponse;
import com.trekmate.backend.dto.response.BookingHistoryResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Booking;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.model.User;
import com.trekmate.backend.model.enums.BookingStatus;
import com.trekmate.backend.repository.BookingRepository;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.repository.UserRepository;
import com.trekmate.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourDepartureRepository departureRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingHistoryResponse> getMyBookings(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Page<Booking> bookings = bookingRepository.findByUserId(user.getId(), pageable);
        return bookings.map(this::mapToHistoryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Bảo mật: Đảm bảo người dùng chỉ xem được booking của chính mình (hoặc là admin)
        if (!booking.getUser().getId().equals(user.getId()) && !user.getIsAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN, "You do not have permission to view this booking");
        }

        return mapToDetailResponse(booking);
    }

    @Override
    @Transactional
    public BookingDetailResponse cancelBooking(Long id, String email, CancelBookingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Bảo mật: Đảm bảo người dùng chỉ hủy được booking của chính mình (hoặc là admin)
        if (!booking.getUser().getId().equals(user.getId()) && !user.getIsAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN, "You do not have permission to cancel this booking");
        }

        BookingStatus currentStatus = booking.getStatus();
        if (currentStatus == BookingStatus.COMPLETED || currentStatus == BookingStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_BE_CANCELLED, 
                    "Booking cannot be cancelled in status: " + currentStatus);
        }

        // Nếu đã thanh toán (CONFIRMED), cần kiểm tra thời hạn hủy (cutoff date)
        if (currentStatus == BookingStatus.CONFIRMED) {
            LocalDate cutoff = booking.getDeparture().getCutoffDate();
            if (cutoff == null) {
                cutoff = booking.getDeparture().getDepartureDate().minusDays(2);
            }
            if (LocalDate.now().isAfter(cutoff)) {
                throw new AppException(ErrorCode.BOOKING_CANNOT_BE_CANCELLED, 
                        "Cannot cancel booking after cutoff date: " + cutoff);
            }
        }

        // Hủy booking
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(request.getReason());
        bookingRepository.save(booking);

        // Giải phóng slots cho TourDeparture
        TourDeparture departure = booking.getDeparture();
        int newSlots = departure.getBookedSlots() - booking.getNumParticipants();
        departure.setBookedSlots((short) Math.max(0, newSlots));
        departureRepository.save(departure);

        log.info("[Booking Cancelled] BookingCode: {} cancelled by User: {}, Slots released: {}", 
                booking.getBookingCode(), user.getEmail(), booking.getNumParticipants());

        return mapToDetailResponse(booking);
    }

    private BookingHistoryResponse mapToHistoryResponse(Booking b) {
        return BookingHistoryResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .tourTitle(b.getDeparture().getTour().getTitle())
                .tourSlug(b.getDeparture().getTour().getSlug())
                .departureDate(b.getDeparture().getDepartureDate())
                .durationDays(b.getDeparture().getTour().getDurationDays())
                .totalPrice(b.getTotalPrice())
                .status(b.getStatus())
                .numParticipants(b.getNumParticipants())
                .bookedAt(b.getBookedAt())
                .build();
    }

    private BookingDetailResponse mapToDetailResponse(Booking b) {
        List<BookingDetailResponse.RentalDetail> rentals = b.getRentals().stream()
                .map(r -> BookingDetailResponse.RentalDetail.builder()
                        .id(r.getId())
                        .equipmentName(r.getEquipment().getName())
                        .brand(r.getEquipment().getBrand())
                        .model(r.getEquipment().getModel())
                        .imageUrl(r.getEquipment().getImageUrl())
                        .quantity(r.getQuantity())
                        .rentalDays(r.getRentalDays())
                        .pricePerDay(r.getPricePerDay())
                        .subtotal(r.getSubtotal())
                        .build())
                .toList();

        List<BookingDetailResponse.PaymentDetail> payments = b.getPayments().stream()
                .map(p -> BookingDetailResponse.PaymentDetail.builder()
                        .id(p.getId())
                        .amount(p.getAmount())
                        .method(p.getMethod())
                        .gatewayTxnId(p.getGatewayTxnId())
                        .status(p.getStatus())
                        .paidAt(p.getPaidAt())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();

        return BookingDetailResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .tourTitle(b.getDeparture().getTour().getTitle())
                .tourSlug(b.getDeparture().getTour().getSlug())
                .departureDate(b.getDeparture().getDepartureDate())
                .returnDate(b.getDeparture().getReturnDate())
                .durationDays(b.getDeparture().getTour().getDurationDays())
                .durationNights(b.getDeparture().getTour().getDurationNights())
                .totalPrice(b.getTotalPrice())
                .subtotalTour(b.getSubtotalTour())
                .subtotalEquipment(b.getSubtotalEquipment())
                .discountAmount(b.getDiscountAmount())
                .status(b.getStatus())
                .numParticipants(b.getNumParticipants())
                .participantsInfo(b.getParticipantsInfo())
                .meetingPoint(b.getDeparture().getMeetingPoint())
                .specialRequests(b.getSpecialRequests())
                .cancellationReason(b.getCancellationReason())
                .cancelledAt(b.getCancelledAt())
                .paidAt(b.getPaidAt())
                .bookedAt(b.getBookedAt())
                .rentals(rentals)
                .payments(payments)
                .build();
    }
}
