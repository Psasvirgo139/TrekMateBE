package com.trekmate.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(5000, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(4000, "Validation error", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(4004, "Resource not found", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(4009, "Resource already exists", HttpStatus.CONFLICT),
    UNAUTHORIZED(4001, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(4003, "Access denied", HttpStatus.FORBIDDEN),

    // User
    USER_NOT_FOUND(4041, "User not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(4091, "Email already exists", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(4092, "Phone number already exists", HttpStatus.CONFLICT),

    // Tour
    TOUR_NOT_FOUND(4042, "Tour not found", HttpStatus.NOT_FOUND),
    TOUR_NOT_AVAILABLE(4021, "Tour is not available for booking", HttpStatus.BAD_REQUEST),
    TOUR_FULLY_BOOKED(4022, "Tour is fully booked for the selected date", HttpStatus.BAD_REQUEST),

    // TourGuide
    TOUR_GUIDE_NOT_FOUND(4043, "Tour guide not found", HttpStatus.NOT_FOUND),
    TOUR_GUIDE_ALREADY_EXISTS(4093, "User is already a tour guide", HttpStatus.CONFLICT),

    // Booking
    BOOKING_NOT_FOUND(4044, "Booking not found", HttpStatus.NOT_FOUND),
    BOOKING_ALREADY_EXISTS(4094, "You have already booked this tour", HttpStatus.CONFLICT),
    BOOKING_CANNOT_BE_CANCELLED(4023, "Booking cannot be cancelled in current status", HttpStatus.BAD_REQUEST),
    DEPARTURE_PAST_CUTOFF(4025, "The cutoff date or departure date has passed", HttpStatus.BAD_REQUEST),

    // Equipment
    EQUIPMENT_NOT_FOUND(4050, "Equipment not found", HttpStatus.NOT_FOUND),
    EQUIPMENT_OUT_OF_STOCK(4024, "Equipment is out of stock or inactive", HttpStatus.BAD_REQUEST),
    EQUIPMENT_CATEGORY_NOT_FOUND(4051, "Equipment category not found", HttpStatus.NOT_FOUND),
    EQUIPMENT_CATEGORY_HAS_ITEMS(4026, "Category has equipment items, cannot delete", HttpStatus.CONFLICT),
    EQUIPMENT_RENTAL_NOT_FOUND(4052, "Equipment rental not found", HttpStatus.NOT_FOUND),
    EQUIPMENT_ALREADY_RETURNED(4027, "This rental has already been returned", HttpStatus.BAD_REQUEST),

    // Review
    REVIEW_NOT_FOUND(4045, "Review not found", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(4095, "You have already reviewed this tour", HttpStatus.CONFLICT),

    // Location
    LOCATION_NOT_FOUND(4046, "Location not found", HttpStatus.NOT_FOUND),
    LOCATION_NAME_ALREADY_EXISTS(4096, "Location name already exists", HttpStatus.CONFLICT),

    // Category
    CATEGORY_NOT_FOUND(4047, "Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_NAME_ALREADY_EXISTS(4097, "Category name already exists", HttpStatus.CONFLICT),

    // PayOS & Payments
    INVALID_REQUEST(4002, "Invalid request", HttpStatus.BAD_REQUEST),
    PAYOS_NOT_CONFIGURED(5001, "PayOS is not configured", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYOS_LINK_FAILED(5002, "Failed to create PayOS payment link", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYOS_PAYMENT_PENDING(4005, "PayOS payment is pending processing", HttpStatus.ACCEPTED),
  
    // Waypoint
    WAYPOINT_NOT_FOUND(4048, "Tour waypoint not found", HttpStatus.NOT_FOUND),
    DUPLICATE_WAYPOINT_ORDER(4098, "Waypoint sequence order already exists in this tour", HttpStatus.CONFLICT),

    // Itinerary
    ITINERARY_NOT_FOUND(4049, "Tour itinerary not found", HttpStatus.NOT_FOUND),
    DUPLICATE_ITINERARY_DAY(4099, "Itinerary day number already exists in this tour", HttpStatus.CONFLICT),

    // Auth
    INVALID_CREDENTIALS(4010, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_SUSPENDED(4011, "Account has been suspended", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED(4012, "Please verify your email before signing in", HttpStatus.FORBIDDEN),
    OTP_INVALID(4013, "Invalid verification code", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(4014, "Verification code has expired", HttpStatus.BAD_REQUEST),
    OTP_NOT_FOUND(4015, "No pending registration found for this email", HttpStatus.NOT_FOUND),
    OTP_MAX_ATTEMPTS(4016, "Too many failed attempts. Please request a new code", HttpStatus.BAD_REQUEST),
    OTP_COOLDOWN(4017, "Please wait before requesting another code", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_SEND_FAILED(5003, "Failed to send verification email", HttpStatus.INTERNAL_SERVER_ERROR),
    GOOGLE_AUTH_FAILED(4018, "Google sign-in failed", HttpStatus.UNAUTHORIZED),
    PASSWORD_RESET_NOT_FOUND(4019, "No password reset request found for this email", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
