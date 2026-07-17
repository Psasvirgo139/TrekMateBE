package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.CertificationDto;
import com.trekmate.backend.dto.request.CustomerProfileUpdateRequest;
import com.trekmate.backend.dto.request.GuideProfileUpdateRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.CustomerProfileResponse;
import com.trekmate.backend.dto.response.GuideProfileResponse;
import com.trekmate.backend.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<CustomerProfileResponse> getCustomerProfile(@PathVariable UUID userId) {
        CustomerProfileResponse data = userProfileService.getCustomerProfile(userId);
        return ApiResponse.<CustomerProfileResponse>builder()
                .code(200)
                .message("Get customer profile successfully")
                .data(data)
                .build();
    }

    @PutMapping("/customer/{userId}")
    @Operation(summary = "Cập nhật thông tin profile Customer")
    public ApiResponse<CustomerProfileResponse> updateCustomerProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        CustomerProfileResponse data = userProfileService.updateCustomerProfile(userId, request);
        return ApiResponse.<CustomerProfileResponse>builder()
                .code(200)
                .message("Update customer profile successfully")
                .data(data)
                .build();
    }

    @GetMapping("/guide/{userId}")
    @Operation(summary = "Lấy thông tin profile Tour Guide")
    public ApiResponse<GuideProfileResponse> getGuideProfile(@PathVariable UUID userId) {
        GuideProfileResponse data = userProfileService.getGuideProfile(userId);
        return ApiResponse.<GuideProfileResponse>builder()
                .code(200)
                .message("Get guide profile successfully")
                .data(data)
                .build();
    }

    @PutMapping("/guide/{userId}")
    @Operation(summary = "Cập nhật thông tin profile Tour Guide")
    public ApiResponse<GuideProfileResponse> updateGuideProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody GuideProfileUpdateRequest request) {
        GuideProfileResponse data = userProfileService.updateGuideProfile(userId, request);
        return ApiResponse.<GuideProfileResponse>builder()
                .code(200)
                .message("Update guide profile successfully")
                .data(data)
                .build();
    }

    @PostMapping("/guide/{userId}/certifications")
    @Operation(summary = "Thêm chứng chỉ cho Tour Guide")
    public ApiResponse<GuideProfileResponse> addGuideCertification(
            @PathVariable UUID userId,
            @Valid @RequestBody CertificationDto certDto) {
        GuideProfileResponse data = userProfileService.addGuideCertification(userId, certDto);
        return ApiResponse.<GuideProfileResponse>builder()
                .code(200)
                .message("Add guide certification successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/guide/{userId}/certifications")
    @Operation(summary = "Xóa chứng chỉ của Tour Guide")
    public ApiResponse<GuideProfileResponse> removeGuideCertification(
            @PathVariable UUID userId,
            @RequestParam String name) {
        GuideProfileResponse data = userProfileService.removeGuideCertification(userId, name);
        return ApiResponse.<GuideProfileResponse>builder()
                .code(200)
                .message("Remove guide certification successfully")
                .data(data)
                .build();
    }
}
