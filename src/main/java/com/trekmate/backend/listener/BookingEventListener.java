package com.trekmate.backend.listener;

import com.trekmate.backend.model.Booking;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listener theo dõi các sự kiện liên quan đến Booking.
 * Có thể mở rộng để gửi email thông báo, push notification, v.v.
 */
@Slf4j
@Component
public class BookingEventListener {

    @PostPersist
    public void onBookingCreated(Booking booking) {
        log.info("[EVENT] Booking created: id={}, code={}, user={}, departure={}, status={}",
                booking.getId(),
                booking.getBookingCode(),
                booking.getUser() != null ? booking.getUser().getId() : "N/A",
                booking.getDeparture() != null ? booking.getDeparture().getId() : "N/A",
                booking.getStatus());
        // TODO: Gửi email xác nhận đặt tour cho user
    }

    @PostUpdate
    public void onBookingUpdated(Booking booking) {
        log.info("[EVENT] Booking updated: id={}, code={}, status={}",
                booking.getId(), booking.getBookingCode(), booking.getStatus());
        // TODO: Gửi thông báo khi trạng thái booking thay đổi
    }
}
