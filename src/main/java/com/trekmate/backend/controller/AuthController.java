package com.trekmate.backend.controller;

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
import com.trekmate.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register, login and current user")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/request-otp")
    @Operation(summary = "Send email OTP for registration")
    public ResponseEntity<SendOtpResponse> requestRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.requestRegistrationOtp(request));
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Verify OTP and complete registration")
    public ResponseEntity<AuthResponse> verifyRegistration(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.verifyRegistration(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    @Operation(summary = "Login or register with Google ID token")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset OTP")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with OTP")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public ResponseEntity<AuthUserResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
