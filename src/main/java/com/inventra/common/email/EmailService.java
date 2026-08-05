package com.inventra.common.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@inventra.com}")
    private String from;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Async
    public void sendOtp(String to, String otp, String name) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, false, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject("Inventra — Your Login OTP");
            h.setText(buildOtpHtml(name, otp), true);
            mailSender.send(msg);
            log.info("OTP email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetLink(String to, String token, String name) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, false, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject("Inventra — Reset Your Password");
            h.setText(buildResetHtml(name, token), true);
            mailSender.send(msg);
            log.info("Password reset email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", to, e.getMessage());
        }
    }

    private String buildOtpHtml(String name, String otp) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;border:1px solid #e2e8f0;border-radius:12px;">
              <div style="text-align:center;margin-bottom:24px;">
                <div style="background:#00bcd4;color:#fff;width:48px;height:48px;border-radius:10px;display:inline-flex;align-items:center;justify-content:center;font-size:20px;font-weight:700;line-height:48px;">IN</div>
                <h2 style="color:#0f172a;margin:12px 0 4px;">Login OTP</h2>
                <p style="color:#64748b;font-size:14px;margin:0;">Your one-time login code for Inventra</p>
              </div>
              <p style="color:#334155;font-size:14px;">Hello <strong>%s</strong>,</p>
              <p style="color:#334155;font-size:14px;">Use the code below to log in. It expires in <strong>10 minutes</strong>.</p>
              <div style="text-align:center;margin:28px 0;">
                <div style="display:inline-block;background:#f1f5f9;border:2px dashed #00bcd4;border-radius:12px;padding:18px 40px;">
                  <span style="font-size:36px;font-weight:800;letter-spacing:10px;color:#0f172a;">%s</span>
                </div>
              </div>
              <p style="color:#94a3b8;font-size:12px;text-align:center;">If you didn't request this, ignore this email. Never share this code.</p>
            </div>
            """.formatted(name != null ? name : "there", otp);
    }

    private String buildResetHtml(String name, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;border:1px solid #e2e8f0;border-radius:12px;">
              <div style="text-align:center;margin-bottom:24px;">
                <div style="background:#00bcd4;color:#fff;width:48px;height:48px;border-radius:10px;display:inline-flex;align-items:center;justify-content:center;font-size:20px;font-weight:700;line-height:48px;">IN</div>
                <h2 style="color:#0f172a;margin:12px 0 4px;">Reset Your Password</h2>
                <p style="color:#64748b;font-size:14px;margin:0;">Inventra Account Security</p>
              </div>
              <p style="color:#334155;font-size:14px;">Hello <strong>%s</strong>,</p>
              <p style="color:#334155;font-size:14px;">We received a request to reset your password. Click the button below — this link expires in <strong>30 minutes</strong>.</p>
              <div style="text-align:center;margin:28px 0;">
                <a href="%s" style="background:#00bcd4;color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:600;font-size:15px;display:inline-block;">Reset Password</a>
              </div>
              <p style="color:#64748b;font-size:12px;">Or paste this link in your browser:</p>
              <p style="color:#00bcd4;font-size:11px;word-break:break-all;">%s</p>
              <p style="color:#94a3b8;font-size:12px;text-align:center;">If you didn't request a reset, ignore this email. Your password won't change.</p>
            </div>
            """.formatted(name != null ? name : "there", resetUrl, resetUrl);
    }
}
