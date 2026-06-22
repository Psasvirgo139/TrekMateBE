package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.BanUserRequest;
import com.trekmate.backend.dto.request.CreateAdminUserRequest;
import com.trekmate.backend.dto.request.UpdateAdminUserRequest;
import com.trekmate.backend.dto.response.AdminUserPageResponse;
import com.trekmate.backend.dto.response.AdminUserResponse;
import com.trekmate.backend.dto.response.AdminUserStatsResponse;
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
    public ResponseEntity<AdminUserPageResponse> listUsers(
            @RequestParam(defaultValue = "ALL") UserRoleFilter role,
            @RequestParam(required = false) UserAccountStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(adminUserService.listUsers(role, status, search, page, size));
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard summary stats")
    public ResponseEntity<AdminUserStatsResponse> getStats() {
        return ResponseEntity.ok(adminUserService.getStats());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getUser(id));
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update basic user info")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminUserRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @PatchMapping("/{id}/ban")
    @Operation(summary = "Suspend user — sets is_active = false")
    public ResponseEntity<AdminUserResponse> banUser(
            @PathVariable UUID id,
            @RequestBody(required = false) BanUserRequest request) {
        return ResponseEntity.ok(adminUserService.banUser(id, request));
    }

    @PatchMapping("/{id}/unban")
    @Operation(summary = "Unban user — sets is_active = true")
    public ResponseEntity<AdminUserResponse> unbanUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.unbanUser(id));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve user — sets is_verified = true")
    public ResponseEntity<AdminUserResponse> approveUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.approveUser(id));
    }
}
