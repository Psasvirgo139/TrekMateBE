package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.CancelBookingRequest;
import com.trekmate.backend.dto.request.CreateBookingRequest;
import com.trekmate.backend.dto.response.BookingDetailResponse;
import com.trekmate.backend.dto.response.BookingHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    Page<BookingHistoryResponse> getMyBookings(String email, Pageable pageable);
    BookingDetailResponse getBookingDetail(Long id, String email);
    BookingDetailResponse cancelBooking(Long id, String email, CancelBookingRequest request);
    BookingDetailResponse createBooking(String email, CreateBookingRequest request);
}
