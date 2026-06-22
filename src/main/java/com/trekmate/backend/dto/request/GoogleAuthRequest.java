package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String idToken,
        Boolean rememberMe
) {
    public boolean isRememberMe() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
