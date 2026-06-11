package com.trekmate.backend.dto.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/** Ket qua 1 query JOIN users + customers + guides — tranh N+1. */
public record AdminUserRow(
        UUID id,
        String email,
        String phone,
        Boolean isAdmin,
        Boolean isActive,
        Boolean isVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime updatedAt,
        String customerFullName,
        String customerAvatarUrl,
        String guideDisplayName,
        String guideAvatarUrl,
        Short experienceYears,
        LocalDateTime profileApprovedAt,
        boolean hasCustomer,
        boolean hasGuide
) {}
