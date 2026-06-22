package com.trekmate.backend.dto.response;

public record SendOtpResponse(
        String message,
        int expiresInMinutes,
        String email
) {}
