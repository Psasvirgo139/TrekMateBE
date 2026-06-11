package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateAdminUserRequest(
        @Email String email,
        String phone,
        @Size(max = 150) String displayName
) {}
