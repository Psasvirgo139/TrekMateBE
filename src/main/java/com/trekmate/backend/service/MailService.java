package com.trekmate.backend.service;

public interface MailService {

    void sendRegistrationOtp(String toEmail, String displayName, String otp, int expiresInMinutes);

    void sendRegistrationSuccess(String toEmail, String displayName);

    void sendPasswordResetOtp(String toEmail, String displayName, String otp, int expiresInMinutes);
}
