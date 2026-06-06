package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.GuideTier;
import com.trekmate.backend.model.enums.UserAccountStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String phone,
        String displayName,
        String avatarUrl,
        List<String> roles,
        GuideTier guideTier,
        UserAccountStatus status,
        LocalDateTime lastActivityAt,
        String lastActivityLabel
) {}
