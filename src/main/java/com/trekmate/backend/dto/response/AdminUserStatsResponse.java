package com.trekmate.backend.dto.response;

public record AdminUserStatsResponse(
        long totalGuides,
        long activeUsers,
        long pendingApproval
) {}
