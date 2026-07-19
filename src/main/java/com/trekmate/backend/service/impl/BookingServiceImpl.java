package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.CancelBookingRequest;
import com.trekmate.backend.dto.request.CreateBookingRequest;
import com.trekmate.backend.dto.response.BookingDetailResponse;
import com.trekmate.backend.dto.response.BookingHistoryResponse;
import com.trekmate.backend.dto.response.WeatherDayResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Booking;
import com.trekmate.backend.model.Equipment;
import com.trekmate.backend.model.EquipmentRental;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.model.User;
import com.trekmate.backend.model.enums.BookingStatus;
import com.trekmate.backend.repository.BookingRepository;
import com.trekmate.backend.repository.EquipmentRentalRepository;
import com.trekmate.backend.repository.EquipmentRepository;
import com.trekmate.backend.repository.TourDepartureRepository;
import com.trekmate.backend.repository.UserRepository;
import com.trekmate.backend.service.BookingService;
import com.trekmate.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourDepartureRepository departureRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentRentalRepository equipmentRentalRepository;
    // Dùng ApplicationContext để tránh circular dependency (WeatherService → BookingService có thể xảy ra)
    private final ApplicationContext applicationContext;

    private WeatherService getWeatherService() {
        return applicationContext.getBean(WeatherService.class);
    }

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

    @Override
    @Transactional
    public BookingDetailResponse createBooking(String email, CreateBookingRequest request) {
        // Validate participants details (Ngày sinh không ở tương lai, Số điện thoại phải đúng 10 chữ số)
        if (request.getParticipantsInfo() != null) {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < request.getParticipantsInfo().size(); i++) {
                java.util.Map<String, Object> p = request.getParticipantsInfo().get(i);
                
                // 1. Validate Date of Birth
                Object dobObj = p.get("dob");
                if (dobObj == null || dobObj.toString().trim().isEmpty()) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Ngày sinh của hành khách #" + (i + 1) + " là bắt buộc");
                }
                try {
                    LocalDate dob = LocalDate.parse(dobObj.toString().trim());
                    if (dob.isAfter(today)) {
                        throw new AppException(ErrorCode.INVALID_REQUEST, "Ngày sinh của hành khách #" + (i + 1) + " không thể ở tương lai");
                    }
                } catch (Exception ex) {
                    if (ex instanceof AppException) {
                        throw (AppException) ex;
                    }
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Định dạng ngày sinh của hành khách #" + (i + 1) + " không hợp lệ");
                }

                // 2. Validate Phone
                Object phoneObj = p.get("phone");
                if (phoneObj == null || phoneObj.toString().trim().isEmpty()) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Số điện thoại của hành khách #" + (i + 1) + " là bắt buộc");
                }
                String phone = phoneObj.toString().trim();
                if (phone.length() != 10 || !phone.matches("\\d{10}")) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Số điện thoại của hành khách #" + (i + 1) + " phải có đúng 10 chữ số");
                }

                // 3. Validate Emergency Contact (phone number only)
                Object emergencyObj = p.get("emergency_contact");
                if (emergencyObj == null || emergencyObj.toString().trim().isEmpty()) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Liên hệ khẩn cấp của hành khách #" + (i + 1) + " là bắt buộc");
                }
                String emergency = emergencyObj.toString().trim();
                if (emergency.length() != 10 || !emergency.matches("\\d{10}")) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Liên hệ khẩn cấp của hành khách #" + (i + 1) + " phải là số điện thoại 10 chữ số");
                }
            }
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TourDeparture departure = departureRepository.findById(request.getDepartureId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Departure not found"));

        // Kiểm tra thời hạn đặt tour
        LocalDate now = LocalDate.now();
        LocalDate cutoff = departure.getCutoffDate();
        if (cutoff == null) {
            cutoff = departure.getDepartureDate().minusDays(2);
        }
        if (now.isAfter(cutoff) || now.isAfter(departure.getDepartureDate())) {
            throw new AppException(ErrorCode.DEPARTURE_PAST_CUTOFF);
        }

        // Kiểm tra số chỗ trống
        int requestedSlots = request.getNumParticipants();
        int currentBooked = departure.getBookedSlots() != null ? departure.getBookedSlots() : 0;
        int maxSlots = departure.getMaxGroupSize() != null ? departure.getMaxGroupSize() : 0;
        if (currentBooked + requestedSlots > maxSlots) {
            throw new AppException(ErrorCode.TOUR_FULLY_BOOKED);
        }

        // Khởi tạo các giá trị thanh toán
        BigDecimal priceSnapshot = departure.getPricePerPerson();
        BigDecimal subtotalTour = priceSnapshot.multiply(BigDecimal.valueOf(requestedSlots));
        BigDecimal subtotalEquipment = BigDecimal.ZERO;
        List<EquipmentRental> rentals = new ArrayList<>();

        // Xử lý thuê thiết bị
        if (request.getRentals() != null && !request.getRentals().isEmpty()) {
            short rentalDays = departure.getTour().getDurationDays() != null ? departure.getTour().getDurationDays() : 1;
            for (var rentalReq : request.getRentals()) {
                Equipment eq = equipmentRepository.findById(rentalReq.getEquipmentId())
                        .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));

                if (eq.getIsActive() == null || !eq.getIsActive() || eq.getAvailableStock() < rentalReq.getQuantity()) {
                    throw new AppException(ErrorCode.EQUIPMENT_OUT_OF_STOCK, "Equipment " + eq.getName() + " is out of stock or inactive");
                }

                // Cập nhật tồn kho khả dụng của thiết bị
                eq.setAvailableStock((short) (eq.getAvailableStock() - rentalReq.getQuantity()));
                equipmentRepository.save(eq);

                BigDecimal itemPrice = eq.getPricePerDay();
                BigDecimal itemSubtotal = itemPrice.multiply(BigDecimal.valueOf(rentalDays))
                        .multiply(BigDecimal.valueOf(rentalReq.getQuantity()));

                EquipmentRental rental = EquipmentRental.builder()
                        .equipment(eq)
                        .quantity(rentalReq.getQuantity())
                        .rentalDays(rentalDays)
                        .pricePerDay(itemPrice)
                        .subtotal(itemSubtotal)
                        .createdAt(LocalDateTime.now())
                        .damageFee(BigDecimal.ZERO)
                        .build();

                rentals.add(rental);
                subtotalEquipment = subtotalEquipment.add(itemSubtotal);
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalPrice = subtotalTour.add(subtotalEquipment).subtract(discountAmount);

        // Sinh mã booking duy nhất (độ dài tối đa 20 ký tự)
        String bookingCode = "BK" + (System.currentTimeMillis() % 1000000L) + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .user(user)
                .departure(departure)
                .numParticipants((short) requestedSlots)
                .participantsInfo(request.getParticipantsInfo())
                .priceSnapshot(priceSnapshot)
                .subtotalTour(subtotalTour)
                .subtotalEquipment(subtotalEquipment)
                .discountAmount(discountAmount)
                .totalPrice(totalPrice)
                .currency("VND")
                .isJoinTour(request.getIsJoinTour())
                .specialRequests(request.getSpecialRequests())
                .status(BookingStatus.PENDING)
                .rentals(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        // Gán liên kết 2 chiều
        for (EquipmentRental r : rentals) {
            r.setBooking(booking);
            booking.getRentals().add(r);
        }

        // Cập nhật bookedSlots của TourDeparture
        departure.setBookedSlots((short) (currentBooked + requestedSlots));
        departureRepository.save(departure);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("[Booking Created] BookingCode: {}, User: {}, TotalPrice: {}", bookingCode, email, totalPrice);

        return mapToDetailResponse(savedBooking);
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

        // Fetch weather forecast (silent fail — không để lỗi weather phá vỡ booking response)
        List<WeatherDayResponse> weatherForecast = List.of();
        try {
            weatherForecast = getWeatherService().getWeatherForDeparture(b.getDeparture().getId());
        } catch (Exception e) {
            log.warn("[Booking] Could not load weather for departure {}: {}", b.getDeparture().getId(), e.getMessage());
        }

        // Weather overview từ TourDeparture (summary ngắn)
        TourDeparture dep = b.getDeparture();

        return BookingDetailResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .tourTitle(dep.getTour().getTitle())
                .tourSlug(dep.getTour().getSlug())
                .departureDate(dep.getDepartureDate())
                .returnDate(dep.getReturnDate())
                .durationDays(dep.getTour().getDurationDays())
                .durationNights(dep.getTour().getDurationNights())
                .totalPrice(b.getTotalPrice())
                .subtotalTour(b.getSubtotalTour())
                .subtotalEquipment(b.getSubtotalEquipment())
                .discountAmount(b.getDiscountAmount())
                .status(b.getStatus())
                .numParticipants(b.getNumParticipants())
                .participantsInfo(b.getParticipantsInfo())
                .meetingPoint(dep.getMeetingPoint())
                .specialRequests(b.getSpecialRequests())
                .cancellationReason(b.getCancellationReason())
                .cancelledAt(b.getCancelledAt())
                .paidAt(b.getPaidAt())
                .bookedAt(b.getBookedAt())
                .departureId(dep.getId())
                .weatherForecast(weatherForecast)
                .weatherOverallSummary(dep.getWeatherSummary())
                .weatherIcon(dep.getWeatherIcon())
                .tempMinC(dep.getTempMinC())
                .tempMaxC(dep.getTempMaxC())
                .rentals(rentals)
                .payments(payments)
                .build();
    }

    @Override
    @Transactional
    public void expirePendingBookings(int expirationMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expirationMinutes);
        List<Booking> pendingBookings = bookingRepository.findByStatusAndBookedAtBefore(
                com.trekmate.backend.model.enums.BookingStatus.PENDING, cutoff);

        if (pendingBookings.isEmpty()) {
            return;
        }

        log.info("Found {} pending bookings to expire", pendingBookings.size());
        for (Booking booking : pendingBookings) {
            try {
                log.info("Expiring booking ID: {}, Code: {}", booking.getId(), booking.getBookingCode());

                // 1. Cập nhật trạng thái booking thành CANCELLED
                booking.setStatus(com.trekmate.backend.model.enums.BookingStatus.CANCELLED);
                booking.setCancellationReason("Hết hạn thanh toán (Auto-expired)");
                booking.setCancelledAt(LocalDateTime.now());

                // 2. Hoàn trả slots cho TourDeparture
                TourDeparture departure = booking.getDeparture();
                short restoredSlots = (short) (departure.getBookedSlots() - booking.getNumParticipants());
                departure.setBookedSlots(restoredSlots >= 0 ? restoredSlots : 0);
                departureRepository.save(departure);

                // 3. Hoàn trả tồn kho đồ thuê
                List<EquipmentRental> rentals = equipmentRentalRepository.findByBookingId(booking.getId());
                for (EquipmentRental rental : rentals) {
                    Equipment eq = rental.getEquipment();
                    eq.setAvailableStock((short) (eq.getAvailableStock() + rental.getQuantity()));
                    equipmentRepository.save(eq);
                }

                bookingRepository.save(booking);
            } catch (Exception e) {
                log.error("Failed to expire booking ID: {}", booking.getId(), e);
            }
        }
    }
}
