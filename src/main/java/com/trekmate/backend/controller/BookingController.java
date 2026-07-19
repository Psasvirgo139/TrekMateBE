package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.CancelBookingRequest;
import com.trekmate.backend.dto.request.CreateBookingRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.BookingDetailResponse;
import com.trekmate.backend.dto.response.BookingHistoryResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.enums.BookingStatus;
import com.trekmate.backend.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking History", description = "API quản lý và truy vấn lịch sử đặt tour & giao dịch của khách hàng")
public class BookingController {

    private final BookingService bookingService;

    /**
     * Helper to resolve the authenticated user's email.
     * Works with standard Spring Security (when JWT filter is fully integrated)
     * and falls back to manual Bearer token parsing (for dev/test environment).
     */
    private String resolveEmail(Authentication authentication, String authHeader) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (token.contains("@")) {
                return token;
            }
        }
        throw new AppException(ErrorCode.UNAUTHORIZED, "User must be authenticated");
    }

    @GetMapping("/my-bookings")
    @Operation(summary = "Lấy lịch sử đặt tour của người dùng hiện tại", description = "Yêu cầu người dùng đăng nhập bằng JWT Token.")
    public ApiResponse<Page<BookingHistoryResponse>> getMyBookings(
            @Parameter(hidden = true) Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        String email = resolveEmail(authentication, authHeader);
        log.info("REST request to get booking history for user: {}, status filter: {}", email, status);
        BookingStatus bookingStatus = null;
        if (status != null && !status.isBlank()) {
            try { bookingStatus = BookingStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ignored) { /* invalid status → no filter */ }
        }
        Page<BookingHistoryResponse> data = bookingService.getMyBookings(email, bookingStatus, pageable);
        return ApiResponse.<Page<BookingHistoryResponse>>builder()
                .code(200)
                .message("Lấy lịch sử đặt tour thành công")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết tour đã đặt theo ID", description = "Trả về thông tin chi tiết của booking gồm danh sách thành viên, đồ thuê, lịch trình và các đợt thanh toán.")
    public ApiResponse<BookingDetailResponse> getBookingDetail(
            @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String email = resolveEmail(authentication, authHeader);
        log.info("REST request to get booking detail: {} for user: {}", id, email);
        BookingDetailResponse data = bookingService.getBookingDetail(id, email);
        return ApiResponse.<BookingDetailResponse>builder()
                .code(200)
                .message("Lấy chi tiết đơn đặt tour thành công")
                .data(data)
                .build();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy tour đã đặt", description = "Hủy tour trước ngày khởi hành (trước cutoffDate). Trạng thái sẽ cập nhật sang CANCELLED và slots khởi hành được giải phóng.")
    public ApiResponse<BookingDetailResponse> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody CancelBookingRequest request,
            @Parameter(hidden = true) Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String email = resolveEmail(authentication, authHeader);
        log.info("REST request to cancel booking: {} by user: {} with reason: {}", id, email, request.getReason());
        BookingDetailResponse data = bookingService.cancelBooking(id, email, request);
        return ApiResponse.<BookingDetailResponse>builder()
                .code(200)
                .message("Hủy đặt tour thành công")
                .data(data)
                .build();
    }

    @PostMapping
    @Operation(summary = "Tạo mới đơn đặt tour", description = "Tạo một đơn đặt tour mới ở trạng thái PENDING. Sau đó người dùng cần gọi API thanh toán.")
    public ApiResponse<BookingDetailResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @Parameter(hidden = true) Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String email = resolveEmail(authentication, authHeader);
        log.info("REST request to create booking by user: {}", email);
        BookingDetailResponse data = bookingService.createBooking(email, request);
        return ApiResponse.<BookingDetailResponse>builder()
                .code(201)
                .message("Tạo đơn đặt tour thành công")
                .data(data)
                .build();
    }
}
