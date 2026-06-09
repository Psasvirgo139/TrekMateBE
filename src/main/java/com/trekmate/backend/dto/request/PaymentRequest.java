package com.trekmate.backend.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long bookingId;
    private String paymentMethod;
    private BigDecimal amount;
    private String transactionCode;
}
