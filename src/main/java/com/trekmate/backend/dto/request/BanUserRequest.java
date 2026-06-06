package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.Size;

public record BanUserRequest(
        @Size(max = 500) String reason
) {}
