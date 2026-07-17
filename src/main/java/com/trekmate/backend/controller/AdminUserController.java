package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.BanUserRequest;
import com.trekmate.backend.dto.request.CreateAdminUserRequest;
import com.trekmate.backend.dto.request.UpdateAdminUserRequest;
import com.trekmate.backend.dto.response.AdminUserPageResponse;
import com.trekmate.backend.dto.response.AdminUserResponse;
import com.trekmate.backend.dto.response.AdminUserStatsResponse;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.model.enums.UserAccountStatus;
import com.trekmate.backend.model.enums.UserRoleFilter;
import com.trekmate.backend.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User management — soft disable via is_active, no hard delete.
 * Requires JWT + ADMIN role.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "User management (Guide Dashboard)")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Paginated user list with role/status/search filters")
    public ApiResponse<AdminUserPageResponse> listUsers(
            @RequestParam(defaultValue = "ALL") UserRoleFilter role,
            @RequestParam(required = false) UserAccountStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AdminUserPageResponse data = adminUserService.listUsers(role, status, search, page, size);
        return ApiResponse.<AdminUserPageResponse>builder()
                .code(200)
                .message("Get users successfully")
                .data(data)
                .build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard summary stats")
    public ApiResponse<AdminUserStatsResponse> getStats() {
        AdminUserStatsResponse data = adminUserService.getStats();
        return ApiResponse.<AdminUserStatsResponse>builder()
                .code(200)
                .message("Get user stats successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable UUID id) {
        AdminUserResponse data = adminUserService.getUser(id);
        return ApiResponse.<AdminUserResponse>builder()
                .code(200)
                .message("Get user successfully")
                .data(data)
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new user")
    public ApiResponse<AdminUserResponse> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        AdminUserResponse data = adminUserService.createUser(request);
        return ApiResponse.<AdminUserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update basic user info")
    public ApiResponse<AdminUserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminUserRequest request) {
        AdminUserResponse data = adminUserService.updateUser(id, request);
        return ApiResponse.<AdminUserResponse>builder()
                .code(200)
                .message("User updated successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{id}/ban")
    @Operation(summary = "Suspend user — sets is_active = false")
    public ApiResponse<AdminUserResponse> banUser(
            @PathVariable UUID id,
            @RequestBody(required = false) BanUserRequest request) {
        AdminUserResponse data = adminUserService.banUser(id, request);
        return ApiResponse.<AdminUserResponse>builder()
                .code(200)
                .message("User banned successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{id}/unban")
    @Operation(summary = "Unban user — sets is_active = true")
    public ApiResponse<AdminUserResponse> unbanUser(@PathVariable UUID id) {
        AdminUserResponse data = adminUserService.unbanUser(id);
        return ApiResponse.<AdminUserResponse>builder()
                .code(200)
                .message("User unbanned successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve user — sets is_verified = true")
    public ApiResponse<AdminUserResponse> approveUser(@PathVariable UUID id) {
        AdminUserResponse data = adminUserService.approveUser(id);
        return ApiResponse.<AdminUserResponse>builder()
                .code(200)
                .message("User approved successfully")
                .data(data)
                .build();
    }
}
