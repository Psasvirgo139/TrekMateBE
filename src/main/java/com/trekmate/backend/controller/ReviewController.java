package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.GuideReplyRequest;
import com.trekmate.backend.dto.request.ReviewRequest;
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
    public ResponseEntity<Page<ReviewResponse>> getReviewsByTour(
            @PathVariable UUID tourId,
            @AuthenticationPrincipal AuthUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String sortBy) {
        UUID currentUserId = principal != null ? principal.getUserId() : null;
        return ResponseEntity.ok(reviewService.getReviewsByTour(tourId, currentUserId, page, size, sortBy));
    }

    @GetMapping("/tour/{tourId}/summary")
    @Operation(summary = "Get review summary (avg ratings + distribution) for a tour")
    public ResponseEntity<ReviewSummaryResponse> getReviewSummary(@PathVariable UUID tourId) {
        return ResponseEntity.ok(reviewService.getReviewSummary(tourId));
    }

    // ─── Authenticated Endpoints ──────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new review for a completed booking")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal AuthUserDetails principal,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(principal.getUserId(), request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my reviews (paginated)")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal AuthUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getReviewsByUser(principal.getUserId(), page, size));
    }

    @PostMapping("/{id}/helpful")
    @Operation(summary = "Toggle helpful vote on a review")
    public ResponseEntity<ReviewResponse> toggleHelpful(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails principal) {
        return ResponseEntity.ok(reviewService.toggleHelpful(id, principal.getUserId()));
    }

    // ─── Guide Endpoints ──────────────────────────────────────────────────────

    @PatchMapping("/{id}/reply")
    @Operation(summary = "Guide replies to a review")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails principal,
            @Valid @RequestBody GuideReplyRequest request) {
        return ResponseEntity.ok(reviewService.replyToReview(id, principal.getUserId(), request));
    }

    // ─── Admin Endpoints ──────────────────────────────────────────────────────

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Admin approves a review")
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
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
