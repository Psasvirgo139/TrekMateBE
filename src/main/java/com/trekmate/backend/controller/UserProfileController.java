package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.CertificationDto;
import com.trekmate.backend.dto.request.CustomerProfileUpdateRequest;
import com.trekmate.backend.dto.request.GuideProfileUpdateRequest;
import com.trekmate.backend.dto.response.CustomerProfileResponse;
import com.trekmate.backend.dto.response.GuideProfileResponse;
import com.trekmate.backend.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Profiles", description = "Endpoints for fetching and updating customer and tour guide profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/customer/{userId}")
    @Operation(summary = "Lấy thông tin profile Customer")
    public ResponseEntity<CustomerProfileResponse> getCustomerProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(userProfileService.getCustomerProfile(userId));
    }

    @PutMapping("/customer/{userId}")
    @Operation(summary = "Cập nhật thông tin profile Customer")
    public ResponseEntity<CustomerProfileResponse> updateCustomerProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.updateCustomerProfile(userId, request));
    }

    @GetMapping("/guide/{userId}")
    @Operation(summary = "Lấy thông tin profile Tour Guide")
    public ResponseEntity<GuideProfileResponse> getGuideProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(userProfileService.getGuideProfile(userId));
    }

    @PutMapping("/guide/{userId}")
    @Operation(summary = "Cập nhật thông tin profile Tour Guide")
    public ResponseEntity<GuideProfileResponse> updateGuideProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody GuideProfileUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.updateGuideProfile(userId, request));
    }

    @PostMapping("/guide/{userId}/certifications")
    @Operation(summary = "Thêm chứng chỉ cho Tour Guide")
    public ResponseEntity<GuideProfileResponse> addGuideCertification(
            @PathVariable UUID userId,
            @Valid @RequestBody CertificationDto certDto) {
        return ResponseEntity.ok(userProfileService.addGuideCertification(userId, certDto));
    }

    @DeleteMapping("/guide/{userId}/certifications")
    @Operation(summary = "Xóa chứng chỉ của Tour Guide")
    public ResponseEntity<GuideProfileResponse> removeGuideCertification(
            @PathVariable UUID userId,
            @RequestParam String name) {
        return ResponseEntity.ok(userProfileService.removeGuideCertification(userId, name));
    }
}
