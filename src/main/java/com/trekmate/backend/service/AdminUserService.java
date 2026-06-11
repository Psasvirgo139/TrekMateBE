package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.BanUserRequest;
import com.trekmate.backend.dto.request.CreateAdminUserRequest;
import com.trekmate.backend.dto.request.UpdateAdminUserRequest;
import com.trekmate.backend.dto.response.AdminUserPageResponse;
import com.trekmate.backend.dto.response.AdminUserResponse;
import com.trekmate.backend.dto.response.AdminUserStatsResponse;
import com.trekmate.backend.model.enums.UserAccountStatus;
import com.trekmate.backend.model.enums.UserRoleFilter;

import java.util.UUID;

public interface AdminUserService {

    AdminUserPageResponse listUsers(
            UserRoleFilter role,
            UserAccountStatus status,
            String search,
            int page,
            int size);

    AdminUserStatsResponse getStats();

    AdminUserResponse getUser(UUID id);

    AdminUserResponse createUser(CreateAdminUserRequest request);

    AdminUserResponse updateUser(UUID id, UpdateAdminUserRequest request);

    AdminUserResponse banUser(UUID id, BanUserRequest request);

    AdminUserResponse unbanUser(UUID id);

    AdminUserResponse approveUser(UUID id);
}
