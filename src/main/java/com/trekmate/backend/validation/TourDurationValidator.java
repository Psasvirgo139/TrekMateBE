package com.trekmate.backend.validation;

import com.trekmate.backend.dto.request.TourRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TourDurationValidator implements ConstraintValidator<ValidTourDuration, TourRequest> {

    @Override
    public boolean isValid(TourRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        Short days = request.durationDays();
        Short nights = request.durationNights();

        // 1. Both must be at least 0, and at least one must be greater than 0
        if (days == null || nights == null || days < 0 || nights < 0 || (days == 0 && nights == 0)) {
            return false;
        }

        // 2. Absolute difference must be 0 or 1
        return Math.abs(days - nights) <= 1;
    }
}
