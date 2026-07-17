package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.GuideReplyRequest;
import com.trekmate.backend.dto.request.ReviewRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.ReviewResponse;
import com.trekmate.backend.dto.response.ReviewSummaryResponse;
import com.trekmate.backend.security.AuthUserDetails;
import com.trekmate.backend.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Tour Review API — CRUD, helpful votes, guide replies, admin approval.
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Tour review management")
public class ReviewController {

    private final ReviewService reviewService;

    // ─── Public Endpoints ─────────────────────────────────────────────────────

    @GetMapping("/tour/{tourId}")
    @Operation(summary = "Get paginated reviews for a tour (approved only)")
    public ApiResponse<Page<ReviewResponse>> getReviewsByTour(
            @PathVariable UUID tourId,
            @AuthenticationPrincipal AuthUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String sortBy) {
        UUID currentUserId = principal != null ? principal.getUserId() : null;
        Page<ReviewResponse> data = reviewService.getReviewsByTour(tourId, currentUserId, page, size, sortBy);
        return ApiResponse.<Page<ReviewResponse>>builder()
                .code(200)
                .message("Get reviews successfully")
                .data(data)
                .build();
    }

    @GetMapping("/tour/{tourId}/summary")
    @Operation(summary = "Get review summary (avg ratings + distribution) for a tour")
    public ApiResponse<ReviewSummaryResponse> getReviewSummary(@PathVariable UUID tourId) {
        ReviewSummaryResponse data = reviewService.getReviewSummary(tourId);
        return ApiResponse.<ReviewSummaryResponse>builder()
                .code(200)
                .message("Get review summary successfully")
                .data(data)
                .build();
    }

    // ─── Authenticated Endpoints ──────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new review for a completed booking")
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal AuthUserDetails principal,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse data = reviewService.createReview(principal.getUserId(), request);
        return ApiResponse.<ReviewResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Review created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/my")
    @Operation(summary = "Get my reviews (paginated)")
    public ApiResponse<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal AuthUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponse> data = reviewService.getReviewsByUser(principal.getUserId(), page, size);
        return ApiResponse.<Page<ReviewResponse>>builder()
                .code(200)
                .message("Get my reviews successfully")
                .data(data)
                .build();
    }

    @PostMapping("/{id}/helpful")
    @Operation(summary = "Toggle helpful vote on a review")
    public ApiResponse<ReviewResponse> toggleHelpful(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails principal) {
        ReviewResponse data = reviewService.toggleHelpful(id, principal.getUserId());
        return ApiResponse.<ReviewResponse>builder()
                .code(200)
                .message("Vote toggled successfully")
                .data(data)
                .build();
    }

    // ─── Guide Endpoints ──────────────────────────────────────────────────────

    @PatchMapping("/{id}/reply")
    @Operation(summary = "Guide replies to a review")
    public ApiResponse<ReviewResponse> replyToReview(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails principal,
            @Valid @RequestBody GuideReplyRequest request) {
        ReviewResponse data = reviewService.replyToReview(id, principal.getUserId(), request);
        return ApiResponse.<ReviewResponse>builder()
                .code(200)
                .message("Guide reply added successfully")
                .data(data)
                .build();
    }

    // ─── Admin Endpoints ──────────────────────────────────────────────────────

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Admin approves a review")
    public ApiResponse<ReviewResponse> approveReview(@PathVariable Long id) {
        ReviewResponse data = reviewService.approveReview(id);
        return ApiResponse.<ReviewResponse>builder()
                .code(200)
                .message("Review approved successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a review (owner or admin)")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails principal) {
        reviewService.deleteReview(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
