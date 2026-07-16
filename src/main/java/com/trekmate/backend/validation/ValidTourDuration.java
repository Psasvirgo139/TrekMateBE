package com.trekmate.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TourDurationValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTourDuration {
    String message() default "Số ngày và số đêm không hợp lý theo thời gian tự nhiên";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}