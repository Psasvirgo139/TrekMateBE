package com.trekmate.backend.dto.request;

import java.util.List;

public record GuideAttendanceRequest(
        List<Long> presentBookingIds,
        List<Long> absentBookingIds
) {}
