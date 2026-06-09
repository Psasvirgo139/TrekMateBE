package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.PaymentRequest;
import com.trekmate.backend.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse makePayment(PaymentRequest request);

    PaymentResponse createPayOSPayment(PaymentRequest request);

    void handlePayOSWebhook(String rawPayload, String signature);

    PaymentResponse confirmPayOsAfterReturn(long orderCode);

    PaymentResponse confirmManualPayment(PaymentRequest request);
}
