package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.FitnessLevel;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID userId,
        String email,
        String phone,
        String fullName,
        String avatarUrl,
        LocalDate dateOfBirth,
        String gender,
        String nationality,
        String homeAddress,
        Map<String, Object> emergencyContact,
        FitnessLevel fitnessLevel,
        String medicalNotes,
        String preferredLanguage,
        Long totalToursJoined
) {}
