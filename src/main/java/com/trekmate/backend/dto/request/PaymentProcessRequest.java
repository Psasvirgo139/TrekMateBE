package com.trekmate.backend.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentProcessRequest {
    private Long bookingId;
    private BigDecimal amount;
    private String paymentMethod;
    private Integer installmentMonths;
}
