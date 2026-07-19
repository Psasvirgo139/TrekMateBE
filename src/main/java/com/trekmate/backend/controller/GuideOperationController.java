package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.GuideAttendanceRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.GuideDepartureResponse;
import com.trekmate.backend.dto.response.GuideParticipantResponse;
import com.trekmate.backend.security.AuthUserDetails;
import com.trekmate.backend.service.GuideOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/guide")
@RequiredArgsConstructor
@Tag(name = "Guide Operations", description = "Endpoints for tour guides to manage their leading departures, check attendance, and complete tours")
public class GuideOperationController {

    private final GuideOperationService guideOperationService;

    @GetMapping("/departures")
    @Operation(summary = "Get list of departures assigned to the logged-in guide")
    public ApiResponse<List<GuideDepartureResponse>> getMyDepartures(@AuthenticationPrincipal AuthUserDetails principal) {
        if (principal == null) {
            return ApiResponse.<List<GuideDepartureResponse>>builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .message("Unauthorized access")
                    .build();
        }
        List<GuideDepartureResponse> departures = guideOperationService.getGuideDepartures(principal.getUserId());
        return ApiResponse.<List<GuideDepartureResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Fetched guide departures successfully")
                .data(departures)
                .build();
    }

    @GetMapping("/departures/{departureId}/participants")
    @Operation(summary = "Get list of participants (bookings) for a specific departure")
    public ApiResponse<List<GuideParticipantResponse>> getParticipants(@PathVariable UUID departureId) {
        List<GuideParticipantResponse> participants = guideOperationService.getDepartureParticipants(departureId);
        return ApiResponse.<List<GuideParticipantResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Fetched participants successfully")
                .data(participants)
                .build();
    }

    @PostMapping("/departures/{departureId}/start")
    @Operation(summary = "Mark a tour departure as ONGOING and perform initial attendance check-in")
    public ApiResponse<Void> startTour(@PathVariable UUID departureId, @RequestBody GuideAttendanceRequest request) {
        guideOperationService.startTour(departureId, request);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Tour started and attendance logged successfully")
                .build();
    }

    @PostMapping("/departures/{departureId}/attendance")
    @Operation(summary = "Edit/update participant attendance during the tour departure")
    public ApiResponse<Void> updateAttendance(@PathVariable UUID departureId, @RequestBody GuideAttendanceRequest request) {
        guideOperationService.updateAttendance(departureId, request);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Attendance updated successfully")
                .build();
    }

    @PostMapping("/departures/{departureId}/complete")
    @Operation(summary = "Mark a tour departure and its ongoing bookings as COMPLETED")
    public ApiResponse<Void> completeTour(@PathVariable UUID departureId) {
        guideOperationService.completeTour(departureId);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Tour marked as completed successfully")
                .build();
    }
}
