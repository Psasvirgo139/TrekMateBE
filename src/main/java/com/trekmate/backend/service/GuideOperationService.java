package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.GuideAttendanceRequest;
import com.trekmate.backend.dto.response.GuideDepartureResponse;
import com.trekmate.backend.dto.response.GuideParticipantResponse;
import java.util.List;
import java.util.UUID;

public interface GuideOperationService {
    List<GuideDepartureResponse> getGuideDepartures(UUID guideId);
    List<GuideParticipantResponse> getDepartureParticipants(UUID departureId);
    void startTour(UUID departureId, GuideAttendanceRequest request);
    void updateAttendance(UUID departureId, GuideAttendanceRequest request);
    void completeTour(UUID departureId);
}
