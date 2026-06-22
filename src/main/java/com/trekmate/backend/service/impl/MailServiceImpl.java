package com.trekmate.backend.service.impl;

import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:TrekMate <duynhatvo05@gmail.com>}")
    private String fromAddress;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetOtp(String toEmail, String displayName, String otp, int expiresInMinutes) {
        ensureMailConfigured();
        String subject = "Reset your TrekMate password";
        String html = buildPasswordResetHtml(displayName, otp, expiresInMinutes);
        sendHtml(toEmail, subject, html);
        log.info("Password reset OTP sent to {}", toEmail);
    }

    @Override
    public void sendRegistrationOtp(String toEmail, String displayName, String otp, int expiresInMinutes) {
        ensureMailConfigured();
        String subject = "Your TrekMate verification code";
        String html = buildOtpHtml(displayName, otp, expiresInMinutes);
        sendHtml(toEmail, subject, html);
        log.info("Registration OTP sent to {} from {}", toEmail, mailUsername);
    }

    @Override
    public void sendRegistrationSuccess(String toEmail, String displayName) {
        ensureMailConfigured();
        String subject = "Welcome to TrekMate — registration complete";
        String html = buildWelcomeHtml(displayName);
        sendHtml(toEmail, subject, html);
        log.info("Registration success email sent to {}", toEmail);
    }

    private void sendHtml(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email to {}", toEmail, ex);
            throw new AppException(
                    ErrorCode.EMAIL_SEND_FAILED,
                    "Could not send email. Check Gmail App Password in application-dev-local.yml"
            );
        }
    }

    private void ensureMailConfigured() {
        if (!StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            throw new AppException(
                    ErrorCode.EMAIL_SEND_FAILED,
                    "Email is not configured. Add Gmail App Password to application-dev-local.yml or set MAIL_PASSWORD"
            );
        }
    }

    private String buildOtpHtml(String displayName, String otp, int expiresInMinutes) {
        String safeName = escapeHtml(displayName);
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,0.06);">
                        <tr><td style="background:#012d1d;padding:24px 28px;">
                          <div style="font-size:20px;font-weight:700;color:#ffffff;">TrekMate</div>
                          <div style="font-size:13px;color:rgba(255,255,255,0.85);margin-top:4px;">Email verification</div>
                        </td></tr>
                        <tr><td style="padding:28px;">
                          <p style="margin:0 0 12px;font-size:16px;">Hi %s,</p>
                          <p style="margin:0 0 20px;line-height:1.6;color:#4b5563;">Use the verification code below to complete your TrekMate registration.</p>
                          <div style="text-align:center;margin:24px 0;">
                            <div style="display:inline-block;padding:16px 28px;border-radius:12px;background:#f8fafc;border:1px solid #e5e7eb;font-size:32px;letter-spacing:8px;font-weight:700;color:#012d1d;">%s</div>
                          </div>
                          <p style="margin:0 0 8px;line-height:1.6;color:#4b5563;">This code expires in <strong>%d minutes</strong>.</p>
                          <p style="margin:0;line-height:1.6;color:#9ca3af;font-size:13px;">If you did not request this, you can safely ignore this email.</p>
                        </td></tr>
                        <tr><td style="padding:18px 28px;background:#f9fafb;border-top:1px solid #eef2f7;color:#9ca3af;font-size:12px;">
                          © TrekMate · Central Vietnam trekking platform
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, otp, expiresInMinutes);
    }

    private String buildWelcomeHtml(String displayName) {
        String safeName = escapeHtml(displayName);
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,0.06);">
                        <tr><td style="background:#012d1d;padding:24px 28px;">
                          <div style="font-size:20px;font-weight:700;color:#ffffff;">TrekMate</div>
                          <div style="font-size:13px;color:rgba(255,255,255,0.85);margin-top:4px;">Registration successful</div>
                        </td></tr>
                        <tr><td style="padding:28px;">
                          <p style="margin:0 0 12px;font-size:16px;">Hi %s,</p>
                          <p style="margin:0 0 16px;line-height:1.6;color:#4b5563;">Your TrekMate account is now active. You can sign in and start exploring guided treks across Central Vietnam.</p>
                          <a href="%s/auth?tab=login" style="display:inline-block;background:#f97316;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:999px;font-weight:700;">Sign in to TrekMate</a>
                        </td></tr>
                        <tr><td style="padding:18px 28px;background:#f9fafb;border-top:1px solid #eef2f7;color:#9ca3af;font-size:12px;">
                          © TrekMate · Central Vietnam trekking platform
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, frontendUrl);
    }

    private String buildPasswordResetHtml(String displayName, String otp, int expiresInMinutes) {
        String safeName = escapeHtml(displayName);
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,0.06);">
                        <tr><td style="background:#012d1d;padding:24px 28px;">
                          <div style="font-size:20px;font-weight:700;color:#ffffff;">TrekMate</div>
                          <div style="font-size:13px;color:rgba(255,255,255,0.85);margin-top:4px;">Password reset</div>
                        </td></tr>
                        <tr><td style="padding:28px;">
                          <p style="margin:0 0 12px;font-size:16px;">Hi %s,</p>
                          <p style="margin:0 0 20px;line-height:1.6;color:#4b5563;">Use this code to reset your TrekMate password.</p>
                          <div style="text-align:center;margin:24px 0;">
                            <div style="display:inline-block;padding:16px 28px;border-radius:12px;background:#f8fafc;border:1px solid #e5e7eb;font-size:32px;letter-spacing:8px;font-weight:700;color:#012d1d;">%s</div>
                          </div>
                          <p style="margin:0 0 8px;line-height:1.6;color:#4b5563;">This code expires in <strong>%d minutes</strong>.</p>
                          <p style="margin:0;line-height:1.6;color:#9ca3af;font-size:13px;">If you did not request this, you can safely ignore this email.</p>
                        </td></tr>
                        <tr><td style="padding:18px 28px;background:#f9fafb;border-top:1px solid #eef2f7;color:#9ca3af;font-size:12px;">
                          © TrekMate · Central Vietnam trekking platform
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, otp, expiresInMinutes);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
