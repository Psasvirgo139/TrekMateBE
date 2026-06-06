package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.projection.AdminUserRow;
import com.trekmate.backend.dto.request.BanUserRequest;
import com.trekmate.backend.dto.request.CreateAdminUserRequest;
import com.trekmate.backend.dto.request.UpdateAdminUserRequest;
import com.trekmate.backend.dto.response.AdminUserPageResponse;
import com.trekmate.backend.dto.response.AdminUserResponse;
import com.trekmate.backend.dto.response.AdminUserStatsResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Customer;
import com.trekmate.backend.model.Guide;
import com.trekmate.backend.model.User;
import com.trekmate.backend.model.UserStatusLog;
import com.trekmate.backend.model.enums.UserAccountStatus;
import com.trekmate.backend.model.enums.UserRoleFilter;
import com.trekmate.backend.repository.AdminUserQueryDao;
import com.trekmate.backend.repository.CustomerRepository;
import com.trekmate.backend.repository.GuideRepository;
import com.trekmate.backend.repository.UserRepository;
import com.trekmate.backend.repository.UserStatusLogRepository;
import com.trekmate.backend.service.AdminUserService;
import com.trekmate.backend.utils.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 50;

    private final AdminUserQueryDao queryDao;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final GuideRepository guideRepository;
    private final UserStatusLogRepository statusLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public AdminUserPageResponse listUsers(
            UserRoleFilter role, UserAccountStatus status, String search, int page, int size) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        UserRoleFilter safeRole = role != null ? role : UserRoleFilter.ALL;

        Page<AdminUserRow> result = queryDao.findUsers(
                safeRole, status, search, PageRequest.of(safePage, safeSize));

        return new AdminUserPageResponse(
                result.getContent().stream().map(AdminUserMapper::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserStatsResponse getStats() {
        return queryDao.fetchStats();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID id) {
        return queryDao.findById(id)
                .map(AdminUserMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public AdminUserResponse createUser(CreateAdminUserRequest request) {
        if (request.role() == UserRoleFilter.ALL) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid role when creating user");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (StringUtils.hasText(request.phone()) && userRepository.existsByPhone(request.phone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        boolean isAdmin = request.role() == UserRoleFilter.ADMIN;
        User user = userRepository.save(User.builder()
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .isVerified(true)
                .isActive(true)
                .isAdmin(isAdmin)
                .build());

        if (request.role() == UserRoleFilter.CUSTOMER || request.role() == UserRoleFilter.GUIDE) {
            customerRepository.save(Customer.builder()
                    .user(user)
                    .fullName(request.displayName())
                    .build());
        }
        if (request.role() == UserRoleFilter.GUIDE) {
            guideRepository.save(Guide.builder()
                    .user(user)
                    .displayName(request.displayName())
                    .profileApprovedAt(LocalDateTime.now())
                    .build());
        }

        return getUser(user.getId());
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(UUID id, UpdateAdminUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (StringUtils.hasText(request.email()) && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.email());
        }
        if (request.phone() != null && !request.phone().equals(user.getPhone())) {
            if (StringUtils.hasText(request.phone()) && userRepository.existsByPhone(request.phone())) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(request.phone());
        }
        if (StringUtils.hasText(request.displayName())) {
            customerRepository.findByUserId(id).ifPresent(c -> c.setFullName(request.displayName()));
            guideRepository.findByUserId(id).ifPresent(g -> g.setDisplayName(request.displayName()));
        }

        return getUser(id);
    }

    @Override
    @Transactional
    public AdminUserResponse banUser(UUID id, BanUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "User is already suspended");
        }

        user.setIsActive(false);
        guideRepository.findByUserId(id).ifPresent(g -> g.setIsAvailable(false));
        saveLog(id, "BAN", request != null ? request.reason() : null);

        return getUser(id);
    }

    @Override
    @Transactional
    public AdminUserResponse unbanUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "User is already active");
        }

        user.setIsActive(true);
        guideRepository.findByUserId(id).ifPresent(g -> g.setIsAvailable(true));
        saveLog(id, "UNBAN", null);

        return getUser(id);
    }

    @Override
    @Transactional
    public AdminUserResponse approveUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setIsVerified(true);
        guideRepository.findByUserId(id).ifPresent(g -> {
            if (g.getProfileApprovedAt() == null) {
                g.setProfileApprovedAt(LocalDateTime.now());
            }
        });
        saveLog(id, "APPROVE", null);

        return getUser(id);
    }

    private void saveLog(UUID userId, String action, String reason) {
        statusLogRepository.save(UserStatusLog.builder()
                .userId(userId)
                .action(action)
                .reason(reason)
                .build());
    }
}
