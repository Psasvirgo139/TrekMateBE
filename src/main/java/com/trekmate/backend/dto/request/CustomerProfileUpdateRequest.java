package com.trekmate.backend.dto.request;

import com.trekmate.backend.model.enums.FitnessLevel;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Map;

public record CustomerProfileUpdateRequest(
        String phone,

        @NotBlank(message = "Full name must not be blank")
        String fullName,

        String avatarUrl,
        LocalDate dateOfBirth,
        String gender,
        String nationality,
        String homeAddress,
        Map<String, Object> emergencyContact,
        FitnessLevel fitnessLevel,
        String medicalNotes,
        String preferredLanguage
) {}
