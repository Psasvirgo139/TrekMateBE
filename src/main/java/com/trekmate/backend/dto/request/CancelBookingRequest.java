package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
