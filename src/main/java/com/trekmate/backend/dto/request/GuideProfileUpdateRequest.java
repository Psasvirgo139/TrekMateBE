package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record GuideProfileUpdateRequest(
        String phone,

        @NotBlank(message = "Display name must not be blank")
        String displayName,

        String avatarUrl,
        String bio,
        String homeProvince,
        Short experienceYears,
        List<String> languages,
        List<String> specializations,
        String idCardNumber,
        Boolean isAvailable
) {}
