package com.trekmate.backend.utils;

import com.trekmate.backend.dto.projection.AdminUserRow;
import com.trekmate.backend.dto.response.AdminUserResponse;
import com.trekmate.backend.model.enums.GuideTier;
import com.trekmate.backend.model.enums.UserAccountStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AdminUserMapper {

    private AdminUserMapper() {}

    public static AdminUserResponse toResponse(AdminUserRow row) {
        LocalDateTime lastActivity = row.lastLoginAt() != null ? row.lastLoginAt() : row.updatedAt();
        return new AdminUserResponse(
                row.id(),
                row.email(),
                row.phone(),
                resolveDisplayName(row),
                resolveAvatarUrl(row),
                resolveRoles(row),
                resolveGuideTier(row),
                resolveStatus(row),
                lastActivity,
                formatRelativeTime(lastActivity)
        );
    }

    public static UserAccountStatus resolveStatus(AdminUserRow row) {
        if (!Boolean.TRUE.equals(row.isActive())) {
            return UserAccountStatus.BANNED;
        }
        if (!Boolean.TRUE.equals(row.isVerified())) {
            return UserAccountStatus.PENDING;
        }
        if (row.hasGuide() && row.profileApprovedAt() == null) {
            return UserAccountStatus.PENDING;
        }
        return UserAccountStatus.ACTIVE;
    }

    public static UserAccountStatus resolveStatus(
            boolean isActive, boolean isVerified, boolean hasGuide, LocalDateTime profileApprovedAt) {
        if (!isActive) return UserAccountStatus.BANNED;
        if (!isVerified) return UserAccountStatus.PENDING;
        if (hasGuide && profileApprovedAt == null) return UserAccountStatus.PENDING;
        return UserAccountStatus.ACTIVE;
    }

    public static List<String> resolveRoles(AdminUserRow row) {
        List<String> roles = new ArrayList<>(3);
        if (Boolean.TRUE.equals(row.isAdmin())) roles.add("ADMIN");
        if (row.hasGuide()) roles.add("GUIDE");
        if (row.hasCustomer()) roles.add("CUSTOMER");
        return roles;
    }

    public static GuideTier resolveGuideTier(AdminUserRow row) {
        if (!row.hasGuide() || row.experienceYears() == null) {
            return null;
        }
        short years = row.experienceYears();
        if (years >= 8) return GuideTier.SENIOR;
        if (years >= 3) return GuideTier.GUIDE;
        return GuideTier.JUNIOR;
    }

    private static String resolveDisplayName(AdminUserRow row) {
        if (row.guideDisplayName() != null && !row.guideDisplayName().isBlank()) {
            return row.guideDisplayName();
        }
        if (row.customerFullName() != null && !row.customerFullName().isBlank()) {
            return row.customerFullName();
        }
        return row.email();
    }

    private static String resolveAvatarUrl(AdminUserRow row) {
        if (row.guideAvatarUrl() != null) return row.guideAvatarUrl();
        return row.customerAvatarUrl();
    }

    /** Relative time label for UI display. */
    public static String formatRelativeTime(LocalDateTime time) {
        if (time == null) return "Never logged in";
        Duration diff = Duration.between(time, LocalDateTime.now());
        long minutes = diff.toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = diff.toHours();
        if (hours < 24) return hours + " hr ago";
        long days = diff.toDays();
        if (days < 30) return days + " days ago";
        return time.toLocalDate().toString();
    }
}
