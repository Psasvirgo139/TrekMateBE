package com.trekmate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CertificationDto(
        @NotBlank(message = "Certification name must not be blank")
        String name,

        @NotBlank(message = "Issued by must not be blank")
        String issuedBy,

        @NotNull(message = "Year must not be null")
        Integer year
) {}
