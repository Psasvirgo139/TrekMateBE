package com.trekmate.backend.controller;

import com.trekmate.backend.dto.request.ForgotPasswordRequest;
import com.trekmate.backend.dto.request.GoogleAuthRequest;
import com.trekmate.backend.dto.request.LoginRequest;
import com.trekmate.backend.dto.request.RegisterRequest;
import com.trekmate.backend.dto.request.ResetPasswordRequest;
import com.trekmate.backend.dto.request.VerifyOtpRequest;
import com.trekmate.backend.dto.response.ApiResponse;
import com.trekmate.backend.dto.response.AuthResponse;
import com.trekmate.backend.dto.response.AuthUserResponse;
import com.trekmate.backend.dto.response.MessageResponse;
import com.trekmate.backend.dto.response.SendOtpResponse;
import com.trekmate.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register, login and current user")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/request-otp")
    @Operation(summary = "Send email OTP for registration")
    public ApiResponse<SendOtpResponse> requestRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        SendOtpResponse data = authService.requestRegistrationOtp(request);
        return ApiResponse.<SendOtpResponse>builder()
                .code(200)
                .message("OTP requested successfully")
                .data(data)
                .build();
    }

    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Verify OTP and complete registration")
    public ApiResponse<AuthResponse> verifyRegistration(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse data = authService.verifyRegistration(request);
        return ApiResponse.<AuthResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Verification successful")
                .data(data)
                .build();
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .message("Login successful")
                .data(data)
                .build();
    }

    @PostMapping("/google")
    @Operation(summary = "Login or register with Google ID token")
    public ApiResponse<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse data = authService.googleLogin(request);
        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .message("Google login successful")
                .data(data)
                .build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset OTP")
    public ApiResponse<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse data = authService.forgotPassword(request);
        return ApiResponse.<MessageResponse>builder()
                .code(200)
                .message("Password reset OTP sent successfully")
                .data(data)
                .build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with OTP")
    public ApiResponse<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse data = authService.resetPassword(request);
        return ApiResponse.<MessageResponse>builder()
                .code(200)
                .message("Password reset successfully")
                .data(data)
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public ApiResponse<AuthUserResponse> me() {
        AuthUserResponse data = authService.getCurrentUser();
        return ApiResponse.<AuthUserResponse>builder()
                .code(200)
                .message("Get current user successfully")
                .data(data)
                .build();
    }
}
