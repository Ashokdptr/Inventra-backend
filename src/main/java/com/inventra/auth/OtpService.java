package com.inventra.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final StringRedisTemplate redis;

    private static final String OTP_PREFIX   = "otp:";
    private static final String RESET_PREFIX = "reset:";
    private static final Duration OTP_TTL    = Duration.ofMinutes(10);
    private static final Duration RESET_TTL  = Duration.ofMinutes(30);

    // ── OTP (login) ─────────────────────────────────────────────────────────

    /** Generate a 6-digit OTP, store it in Redis, return it */
    public String generateOtp(String email) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        redis.opsForValue().set(OTP_PREFIX + email.toLowerCase(), otp, OTP_TTL);
        log.debug("OTP generated for {}", email);
        return otp;
    }

    /** Verify OTP — returns true and deletes if valid */
    public boolean verifyOtp(String email, String otp) {
        String key   = OTP_PREFIX + email.toLowerCase();
        String stored = redis.opsForValue().get(key);
        if (stored != null && stored.equals(otp)) {
            redis.delete(key);
            return true;
        }
        return false;
    }

    // ── Password Reset ───────────────────────────────────────────────────────

    /** Generate a UUID reset token, store email→token mapping, return token */
    public String generateResetToken(String email) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(RESET_PREFIX + token, email.toLowerCase(), RESET_TTL);
        return token;
    }

    /** Return the email for a valid reset token, or null */
    public String getEmailForResetToken(String token) {
        return redis.opsForValue().get(RESET_PREFIX + token);
    }

    /** Delete used reset token */
    public void deleteResetToken(String token) {
        redis.delete(RESET_PREFIX + token);
    }
}
