package com.trekmate.backend.dto.request;

import com.trekmate.backend.model.enums.RegisterRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank String displayName,
        String phone,
        @NotNull RegisterRole role
) {}
