package com.trekmate.backend.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayOSPaymentResponse {
    private String bookingCode;
    private Long orderCode;
    private BigDecimal amount;
    private String checkoutUrl;
    private String status;
}
