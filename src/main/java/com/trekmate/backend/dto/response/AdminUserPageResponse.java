package com.trekmate.backend.dto.response;

import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
