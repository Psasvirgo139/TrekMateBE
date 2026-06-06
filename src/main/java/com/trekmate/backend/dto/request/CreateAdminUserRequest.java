package com.trekmate.backend.dto.request;

import com.trekmate.backend.model.enums.UserRoleFilter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @NotBlank @Email String email,
        String phone,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 150) String displayName,
        @NotNull UserRoleFilter role
) {}
