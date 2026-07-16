package com.trekmate.backend.validation;

import com.trekmate.backend.dto.request.TourRequest;
import com.trekmate.backend.model.enums.DifficultyLevel;
import com.trekmate.backend.model.enums.TourStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TourRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private TourRequest createValidRequest() {
        return new TourRequest(
                "Trekking Ta Nang Phan Dung",
                "ta-nang-phan-dung",
                "Short description",
                "Full description of the tour",
                DifficultyLevel.MODERATE,
                (short) 3,
                (short) 2, // 3 days 2 nights -> valid
                BigDecimal.valueOf(30),
                1000,
                "Lam Dong",
                "Binh Thuan",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "http://gpx.url",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                TourStatus.DRAFT
        );
    }

    @Test
    void whenRequestIsValid_thenNoViolations() {
        TourRequest request = createValidRequest();
        Set<ConstraintViolation<TourRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenDaysAndNightsDifferenceIsTwo_thenViolation() {
        TourRequest request = new TourRequest(
                "Trekking Ta Nang Phan Dung",
                "ta-nang-phan-dung",
                "Short description",
                "Full description of the tour",
                DifficultyLevel.MODERATE,
                (short) 4,
                (short) 2, // 4 days 2 nights -> difference is 2, should fail
                BigDecimal.valueOf(30),
                1000,
                "Lam Dong",
                "Binh Thuan",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "http://gpx.url",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                TourStatus.DRAFT
        );

        Set<ConstraintViolation<TourRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Số ngày và số đêm không hợp lý")));
    }

    @Test
    void whenBothDaysAndNightsAreZero_thenViolation() {
        TourRequest request = new TourRequest(
                "Trekking Ta Nang Phan Dung",
                "ta-nang-phan-dung",
                "Short description",
                "Full description of the tour",
                DifficultyLevel.MODERATE,
                (short) 0, // days = 0
                (short) 0, // nights = 0 -> both 0 should fail
                BigDecimal.valueOf(30),
                1000,
                "Lam Dong",
                "Binh Thuan",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "http://gpx.url",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                TourStatus.DRAFT
        );

        Set<ConstraintViolation<TourRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void whenOneDayZeroNights_thenValid() {
        TourRequest request = new TourRequest(
                "Trekking Ta Nang Phan Dung",
                "ta-nang-phan-dung",
                "Short description",
                "Full description of the tour",
                DifficultyLevel.MODERATE,
                (short) 1,
                (short) 0, // 1 day 0 nights -> valid
                BigDecimal.valueOf(30),
                1000,
                "Lam Dong",
                "Binh Thuan",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "http://gpx.url",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                TourStatus.DRAFT
        );

        Set<ConstraintViolation<TourRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenZeroDaysOneNight_thenValid() {
        TourRequest request = new TourRequest(
                "Trekking Ta Nang Phan Dung",
                "ta-nang-phan-dung",
                "Short description",
                "Full description of the tour",
                DifficultyLevel.MODERATE,
                (short) 0,
                (short) 1, // 0 days 1 night -> valid
                BigDecimal.valueOf(30),
                1000,
                "Lam Dong",
                "Binh Thuan",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "http://gpx.url",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                TourStatus.DRAFT
        );

        Set<ConstraintViolation<TourRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenLocationIsBlank_thenViolation() {
        TourRequest request = new TourRequest(
                "Trekking Ta Nang Phan Dung",
                "ta-nang-phan-dung",
                "Short description",
                "Full description of the tour",
                DifficultyLevel.MODERATE,
                (short) 3,
                (short) 2,
                BigDecimal.valueOf(30),
                1000,
                "   ", // blank location -> should fail
                "Binh Thuan",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "http://gpx.url",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                TourStatus.DRAFT
        );

        Set<ConstraintViolation<TourRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("startLocation")));
    }

    @Test
    void whenLocationContainsInvalidCharacters_thenViolation() {
        TourRequest request = new TourRequest(
                "Trekking Ta Nang Phan Dung",
                "ta-nang-phan-dung",
                "Short description",
                "Full description of the tour",
                DifficultyLevel.MODERATE,
                (short) 3,
                (short) 2,
                BigDecimal.valueOf(30),
                1000,
                "Lam Dong #123", // invalid chars like #, digits (assuming locations must contain only letters/spaces/punctuation) -> should fail
                "Binh Thuan",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "http://gpx.url",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                TourStatus.DRAFT
        );

        Set<ConstraintViolation<TourRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("startLocation")));
    }
}
