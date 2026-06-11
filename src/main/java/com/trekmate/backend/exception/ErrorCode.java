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
    DUPLICATE_ITINERARY_DAY(4099, "Itinerary day number already exists in this tour", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
