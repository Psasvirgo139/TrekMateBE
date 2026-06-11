package com.trekmate.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trekmate.backend.dto.request.PaymentRequest;
import com.trekmate.backend.dto.response.PaymentResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Booking;
import com.trekmate.backend.model.Payment;
import com.trekmate.backend.model.PaymentLog;
import com.trekmate.backend.model.enums.BookingStatus;
import com.trekmate.backend.repository.BookingRepository;
import com.trekmate.backend.repository.PaymentLogRepository;
import com.trekmate.backend.repository.PaymentRepository;
import com.trekmate.backend.service.PayOSService;
import com.trekmate.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final PayOSService payOSService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentResponse makePayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod(request.getPaymentMethod());
        payment.setGatewayTxnId(request.getTransactionCode());
        payment.setCreatedAt(LocalDateTime.now());

        if (request.getAmount() != null) {
            payment.setAmount(request.getAmount());
            payment.setStatus("PENDING");
        } else {
            payment.setAmount(booking.getTotalPrice());
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaidAt(LocalDateTime.now());
            awardLoyaltyAndSendMail(booking, payment.getAmount());
        }

        Payment saved = paymentRepository.save(payment);
        bookingRepository.save(booking);
        savePaymentLog(saved, "Manual payment created");

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse createPayOSPayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        BigDecimal payAmount = request.getAmount() != null ? request.getAmount() : booking.getTotalPrice();
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        long orderCode = booking.getId() * 1000 + (System.currentTimeMillis() % 1000);
        String transactionCode = "PAYOS-" + orderCode;
        int amount = payAmount.intValue();

        String checkoutUrl = payOSService.createPaymentLink(
                orderCode,
                amount,
                "Booking #" + booking.getId());

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(payAmount);
        payment.setMethod("PAYOS");
        payment.setGatewayTxnId(transactionCode);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        Payment saved = paymentRepository.save(payment);
        savePaymentLog(saved, "PayOS link created");

        return PaymentResponse.builder()
                .paymentId(saved.getId())
                .bookingId(booking.getId())
                .amount(saved.getAmount())
                .paymentMethod(saved.getMethod())
                .status(saved.getStatus())
                .checkoutUrl(checkoutUrl)
                .orderCode(orderCode)
                .build();
    }

    @Override
    @Transactional
    public void handlePayOSWebhook(String rawPayload, String headerSignature) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        JsonNode data = root.path("data");
        String signature = (headerSignature != null && !headerSignature.isBlank())
                ? headerSignature.trim()
                : root.path("signature").asText("").trim();
        if (!payOSService.verifyPayOsDataSignature(data, signature)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (!data.isObject() || !data.has("orderCode") || data.get("orderCode").isNull()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        long orderCode = data.path("orderCode").asLong();
        String transactionCode = "PAYOS-" + orderCode;
        Payment payment = paymentRepository.findByGatewayTxnId(transactionCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        // Check expiration before processing webhook
        if (payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(LocalDateTime.now())) {
            savePaymentLog(payment, "Payment expired before webhook processing");
            throw new AppException(ErrorCode.PAYOS_PAYMENT_PENDING);
        }


        if (!PayOSService.amountsMatch(payment.getAmount(), data)) {
            savePaymentLog(payment, "PayOS webhook: amount mismatch — không cập nhật");
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (isPayOsPaidFromWebhook(root, data)) {
            finalizePayOsPaymentSuccess(payment, "PayOS webhook (VietQR) thành công");
            return;
        }

        if (isPayOsCancelledOrFailedFromWebhook(root, data)) {
            if ("PENDING".equals(payment.getStatus())) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                savePaymentLog(payment, "PayOS webhook: hủy / thất bại");
            }
        }
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayOsAfterReturn(long orderCode) {
        Optional<JsonNode> apiDataOpt = payOSService.fetchPaymentRequestByOrderCode(orderCode);
        if (apiDataOpt.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        JsonNode d = apiDataOpt.get();
        String status = d.path("status").asText("").trim().toUpperCase(Locale.ROOT);
        String transactionCode = "PAYOS-" + orderCode;
        Payment payment = paymentRepository.findByGatewayTxnId(transactionCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

        if (!PayOSService.amountsMatch(payment.getAmount(), d)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Check expiration
        if (payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(LocalDateTime.now())) {
            savePaymentLog(payment, "Payment expired after hold period");
            throw new AppException(ErrorCode.PAYOS_PAYMENT_PENDING);
        }
        if ("PAID".equals(status)) {
            finalizePayOsPaymentSuccess(payment, "PayOS returnUrl + API: PAID");
            return toResponse(paymentRepository.findById(payment.getId()).orElse(payment));
        }
        if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
            throw new AppException(ErrorCode.PAYOS_PAYMENT_PENDING);
        }
        if ("CANCELLED".equals(status) || "FAILED".equals(status)) {
            if ("PENDING".equals(payment.getStatus())) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                savePaymentLog(payment, "PayOS API: " + status);
            }
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    @Override
    @Transactional
    public PaymentResponse confirmManualPayment(PaymentRequest request) {
        if (request == null || request.getBookingId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Payment payment = paymentRepository
                .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), "PENDING")
                .or(() -> paymentRepository.findFirstByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .orElseGet(() -> {
                    Payment created = new Payment();
                    created.setBooking(booking);
                    created.setAmount(booking.getTotalPrice());
                    created.setMethod(
                            request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                                    ? request.getPaymentMethod()
                                    : "BANK_TRANSFER");
                    created.setExpiresAt(LocalDateTime.now().plusMinutes(10));
                    created.setStatus("PENDING");
                    created.setCreatedAt(LocalDateTime.now());
                    Payment saved = paymentRepository.save(created);
                    savePaymentLog(saved, "Manual payment created on confirm flow");
                    return saved;
                });

        if (!"SUCCESS".equals(payment.getStatus())) {
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaidAt(LocalDateTime.now());
            bookingRepository.save(booking);
            paymentRepository.save(payment);
            awardLoyaltyAndSendMail(booking, payment.getAmount());
            savePaymentLog(payment, "Manual transfer confirmed by operator");
        }

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment saved) {
        return PaymentResponse.builder()
                .paymentId(saved.getId())
                .bookingId(saved.getBooking().getId())
                .amount(saved.getAmount())
                .paymentMethod(saved.getMethod())
                .status(saved.getStatus())
                .build();
    }

    private void savePaymentLog(Payment payment, String message) {
        PaymentLog logEntity = new PaymentLog();
        logEntity.setPayment(payment);
        logEntity.setLogMessage(message + " | amount=" + payment.getAmount() + " | method=" + payment.getMethod());
        paymentLogRepository.save(logEntity);
    }

    private void awardLoyaltyAndSendMail(Booking booking, BigDecimal paidAmount) {
        String fullName = "Unknown";
        if (booking.getUser() != null && booking.getUser().getCustomer() != null) {
            fullName = booking.getUser().getCustomer().getFullName();
        }
        log.info("[Payment Success Log] Booking successfully paid! BookingId: {}, Amount: {}, Customer: {}",
                booking.getId(), paidAmount, fullName);
        // LoyaltyService & MailService are mocked for now since they are not present in TrekMate codebase.
    }

    private void finalizePayOsPaymentSuccess(Payment payment, String logMessage) {
        if ("SUCCESS".equals(payment.getStatus())) {
            return;
        }
        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaidAt(LocalDateTime.now());
        bookingRepository.save(booking);
        paymentRepository.save(payment);
        awardLoyaltyAndSendMail(booking, payment.getAmount());
        savePaymentLog(payment, logMessage);
    }

    private static boolean isPayOsPaidFromWebhook(JsonNode root, JsonNode data) {
        boolean outerOk = "00".equals(root.path("code").asText()) && root.path("success").asBoolean(false);
        if (!outerOk) {
            return false;
        }
        String dataStatus = data.path("status").asText("");
        if ("PAID".equalsIgnoreCase(dataStatus) || "SUCCESS".equalsIgnoreCase(dataStatus)) {
            return true;
        }
        String innerCode = data.path("code").asText("");
        return innerCode.isEmpty() || "00".equals(innerCode);
    }

    private static boolean isPayOsCancelledOrFailedFromWebhook(JsonNode root, JsonNode data) {
        if (!root.path("success").asBoolean(true)) {
            return true;
        }
        String dataStatus = data.path("status").asText("");
        return "CANCELLED".equalsIgnoreCase(dataStatus) || "FAILED".equalsIgnoreCase(dataStatus);
    }
}
