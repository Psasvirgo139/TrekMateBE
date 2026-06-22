package com.trekmate.backend.dto.response;

import java.util.List;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String displayName,
        List<String> roles,
        String status
) {}
