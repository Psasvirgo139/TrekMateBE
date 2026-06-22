package com.trekmate.backend.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserResponse user
) {}
