package com.trekmate.backend.dto.response;

import com.trekmate.backend.model.enums.BookingStatus;

public record GuideParticipantResponse(
        Long bookingId,
        String bookingCode,
        String customerName,
        String email,
        String phone,
        int numParticipants,
        BookingStatus status
) {}
