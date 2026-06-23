package com.trekmate.backend.service;

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

public interface AuthService {

    SendOtpResponse requestRegistrationOtp(RegisterRequest request);

    AuthResponse verifyRegistration(VerifyOtpRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleAuthRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    AuthUserResponse getCurrentUser();
}
