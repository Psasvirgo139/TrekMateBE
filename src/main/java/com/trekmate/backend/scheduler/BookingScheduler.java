package com.trekmate.backend.scheduler;

import com.trekmate.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduler {

    private final BookingService bookingService;

    /**
     * Tác vụ quét ngầm chạy mỗi 1 phút để hủy các đơn đặt tour PENDING quá 30 phút.
     */
    @Scheduled(fixedRate = 60000)
    public void expirePendingBookings() {
        log.debug("Scheduling task: Scanning for expired pending bookings...");
        try {
            bookingService.expirePendingBookings(30);
        } catch (Exception e) {
            log.error("Error occurred while running expired pending bookings scheduler", e);
        }
    }
}
