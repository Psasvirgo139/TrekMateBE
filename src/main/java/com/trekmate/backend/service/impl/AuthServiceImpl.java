package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.ForgotPasswordRequest;
import com.trekmate.backend.dto.request.GoogleAuthRequest;
import com.trekmate.backend.dto.request.LoginRequest;
import com.trekmate.backend.dto.request.RegisterRequest;
import com.trekmate.backend.dto.request.ResetPasswordRequest;
import com.trekmate.backend.dto.request.VerifyOtpRequest;
import com.trekmate.backend.dto.response.AuthResponse;
import com.trekmate.backend.dto.response.AuthUserResponse;
import com.trekmate.backend.dto.response.MessageResponse;
import com.trekmate.backend.dto.response.SendOtpResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.model.Customer;
import com.trekmate.backend.model.Guide;
import com.trekmate.backend.model.PasswordResetToken;
import com.trekmate.backend.model.RegistrationOtp;
import com.trekmate.backend.model.User;
import com.trekmate.backend.model.enums.RegisterRole;
import com.trekmate.backend.repository.CustomerRepository;
import com.trekmate.backend.repository.GuideRepository;
import com.trekmate.backend.repository.PasswordResetTokenRepository;
import com.trekmate.backend.repository.RegistrationOtpRepository;
import com.trekmate.backend.repository.UserRepository;
import com.trekmate.backend.security.AuthUserDetails;
import com.trekmate.backend.security.GoogleTokenVerifier;
import com.trekmate.backend.security.GoogleTokenVerifier.GoogleUserInfo;
import com.trekmate.backend.security.JwtService;
import com.trekmate.backend.service.AuthService;
import com.trekmate.backend.service.MailService;
import com.trekmate.backend.utils.AuthUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final GuideRepository guideRepository;
    private final RegistrationOtpRepository registrationOtpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthUserMapper authUserMapper;
    private final MailService mailService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Value("${app.mail.otp-expiration-minutes:30}")
    private int otpExpirationMinutes;

    @Value("${app.mail.otp-cooldown-seconds:60}")
    private int otpCooldownSeconds;

    @Value("${app.mail.otp-max-attempts:5}")
    private int otpMaxAttempts;

    @Override
    @Transactional
    public SendOtpResponse requestRegistrationOtp(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (StringUtils.hasText(request.phone()) && userRepository.existsByPhone(request.phone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        Optional<RegistrationOtp> existing = registrationOtpRepository.findById(email);
        if (existing.isPresent()) {
            enforceOtpCooldown(existing.get().getLastRequestedAt());
        }

        String otp = generateOtp();
        RegistrationOtp pending = RegistrationOtp.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName().trim())
                .phone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null)
                .role(request.role())
                .failedAttempts(0)
                .lastRequestedAt(LocalDateTime.now())
                .build();

        registrationOtpRepository.save(pending);
        mailService.sendRegistrationOtp(email, pending.getDisplayName(), otp, otpExpirationMinutes);

        return new SendOtpResponse(
                "Verification code sent to your email.",
                otpExpirationMinutes,
                email
        );
    }

    @Override
    @Transactional
    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
        RegistrationOtp pending = registrationOtpRepository.findById(email)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_NOT_FOUND));

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            registrationOtpRepository.delete(pending);
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        if (!passwordEncoder.matches(request.otp(), pending.getOtpHash())) {
            int attempts = pending.getFailedAttempts() + 1;
            pending.setFailedAttempts(attempts);
            if (attempts >= otpMaxAttempts) {
                registrationOtpRepository.delete(pending);
                throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS);
            }
            registrationOtpRepository.save(pending);
            throw new AppException(ErrorCode.OTP_INVALID);
        }
        if (userRepository.existsByEmail(email)) {
            registrationOtpRepository.delete(pending);
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        boolean isGuide = pending.getRole() == RegisterRole.GUIDE;
        User user = userRepository.save(User.builder()
                .email(email)
                .phone(pending.getPhone())
                .passwordHash(pending.getPasswordHash())
                .isVerified(true)
                .isActive(true)
                .isAdmin(false)
                .build());

        customerRepository.save(Customer.builder()
                .user(user)
                .fullName(pending.getDisplayName())
                .build());

        if (isGuide) {
            guideRepository.save(Guide.builder()
                    .user(user)
                    .displayName(pending.getDisplayName())
                    .build());
        }

        registrationOtpRepository.delete(pending);
        mailService.sendRegistrationSuccess(email, pending.getDisplayName());

        return buildAuthResponse(user, false);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        ensureLoginAllowed(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user, request.isRememberMe());
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleAuthRequest request) {
        GoogleUserInfo googleUser = googleTokenVerifier.verify(request.idToken());
        String email = normalizeEmail(googleUser.email());

        Optional<User> byGoogleId = userRepository.findByGoogleId(googleUser.googleId());
        if (byGoogleId.isPresent()) {
            User user = byGoogleId.get();
            ensureLoginAllowed(user);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            return buildAuthResponse(user, request.isRememberMe());
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            ensureLoginAllowed(user);
            user.setGoogleId(googleUser.googleId());
            user.setIsVerified(true);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            return buildAuthResponse(user, request.isRememberMe());
        }

        String displayName = StringUtils.hasText(googleUser.displayName())
                ? googleUser.displayName().trim()
                : email;

        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .googleId(googleUser.googleId())
                .isVerified(true)
                .isActive(true)
                .isAdmin(false)
                .lastLoginAt(LocalDateTime.now())
                .build());

        customerRepository.save(Customer.builder()
                .user(user)
                .fullName(displayName)
                .build());

        mailService.sendRegistrationSuccess(email, displayName);
        return buildAuthResponse(user, request.isRememberMe());
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return genericResetMessage();
            }

            Optional<PasswordResetToken> existing = passwordResetTokenRepository.findById(email);
            if (existing.isPresent()) {
                enforceOtpCooldown(existing.get().getLastRequestedAt());
            }

            String otp = generateOtp();
            String displayName = authUserMapper.toResponse(user).displayName();
            PasswordResetToken token = PasswordResetToken.builder()
                    .email(email)
                    .otpHash(passwordEncoder.encode(otp))
                    .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                    .failedAttempts(0)
                    .lastRequestedAt(LocalDateTime.now())
                    .build();
            passwordResetTokenRepository.save(token);
            mailService.sendPasswordResetOtp(email, displayName, otp, otpExpirationMinutes);
        }

        return genericResetMessage();
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        PasswordResetToken token = passwordResetTokenRepository.findById(email)
                .orElseThrow(() -> new AppException(ErrorCode.PASSWORD_RESET_NOT_FOUND));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(token);
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        if (!passwordEncoder.matches(request.otp(), token.getOtpHash())) {
            int attempts = token.getFailedAttempts() + 1;
            token.setFailedAttempts(attempts);
            if (attempts >= otpMaxAttempts) {
                passwordResetTokenRepository.delete(token);
                throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS);
            }
            passwordResetTokenRepository.save(token);
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setIsVerified(true);
        userRepository.save(user);
        passwordResetTokenRepository.delete(token);

        return new MessageResponse("Password updated successfully. You can sign in now.");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser() {
        User user = getAuthenticatedUser();
        return authUserMapper.toResponse(user);
    }

    private void ensureLoginAllowed(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void enforceOtpCooldown(LocalDateTime lastRequestedAt) {
        if (lastRequestedAt != null
                && lastRequestedAt.plusSeconds(otpCooldownSeconds).isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_COOLDOWN,
                    "Please wait " + otpCooldownSeconds + " seconds before requesting another code");
        }
    }

    private MessageResponse genericResetMessage() {
        return new MessageResponse(
                "If an account exists for this email, a password reset code has been sent.");
    }

    private AuthResponse buildAuthResponse(User user, boolean rememberMe) {
        List<String> roles = authUserMapper.resolveRoles(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail(), roles, rememberMe);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(rememberMe),
                authUserMapper.toResponse(user)
        );
    }

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthUserDetails authUserDetails)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return authUserDetails.getUser();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String generateOtp() {
        return String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
    }
}
