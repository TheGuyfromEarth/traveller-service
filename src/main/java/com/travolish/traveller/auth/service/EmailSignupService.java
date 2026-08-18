package com.travolish.traveller.auth.service;

import com.travolish.traveller.auth.dto.EmailSignupStartRequest;
import com.travolish.traveller.auth.dto.EmailSignupVerifyRequest;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;
import com.travolish.traveller.auth.entity.EmailVerificationCode;
import com.travolish.traveller.auth.repository.EmailVerificationCodeRepository;
import com.travolish.traveller.auth.util.JwtTokenProvider;
import com.travolish.traveller.notifications.service.EmailService;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Passwordless email sign-up: emails a one-time code, verifies it, and creates
 * the user — issuing the same JWT pair as the OAuth2 flows so the frontend
 * session handling is identical.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSignupService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpirationMs;

    /**
     * Generate a code, persist it, and email it. Rejects emails that already
     * have an account.
     */
    @Transactional
    public void startSignup(EmailSignupStartRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("An account with this email already exists. Please sign in instead.");
        }

        String code = generateCode();
        EmailVerificationCode entity = EmailVerificationCode.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .consumed(false)
                .attempts(0)
                .requestedRole(normalizeRole(request.getRole()))
                .build();
        codeRepository.save(entity);

        emailService.sendHtmlEmail(email, "Your Travolish verification code", buildEmailBody(code));
        log.info("Signup verification code issued for {}", email);
    }

    /**
     * Validate the supplied code and, on success, create the user and return an
     * authenticated session.
     */
    @Transactional
    public OAuth2AuthResponse verifyAndCreate(EmailSignupVerifyRequest request) {
        String email = normalizeEmail(request.getEmail());

        EmailVerificationCode record = codeRepository.findLatestActiveByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No active verification code. Please request a new code."));

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            record.setConsumed(true);
            codeRepository.save(record);
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }

        if (record.getAttempts() >= MAX_ATTEMPTS) {
            record.setConsumed(true);
            codeRepository.save(record);
            throw new IllegalArgumentException("Too many incorrect attempts. Please request a new code.");
        }

        if (!record.getCode().equals(request.getCode().trim())) {
            record.setAttempts(record.getAttempts() + 1);
            codeRepository.save(record);
            throw new IllegalArgumentException("Incorrect verification code.");
        }

        // Guard against a race where the account was created between start and verify
        if (userRepository.existsByEmail(email)) {
            record.setConsumed(true);
            codeRepository.save(record);
            throw new IllegalStateException("An account with this email already exists. Please sign in instead.");
        }

        record.setConsumed(true);
        codeRepository.save(record);

        String role = record.getRequestedRole() != null ? record.getRequestedRole() : "GUEST";

        // Support two name-entry modes coming from the frontend:
        //   Nickname mode  → preferredName is set, firstName/lastName may be absent
        //   Formal mode    → firstName + lastName are set, preferredName is absent
        String firstName     = request.getFirstName() != null ? request.getFirstName().trim() : "";
        String lastName      = request.getLastName()  != null ? request.getLastName().trim()  : "";
        String preferredName = request.getPreferredName() != null ? request.getPreferredName().trim() : null;

        User user = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .preferredName(preferredName)
                .provider("email")
                .role(role)
                .status("ACTIVE")
                .build();
        User saved = userRepository.save(user);
        log.info("User {} created via email signup", saved.getId());

        String accessToken = jwtTokenProvider.generateToken(saved);
        String refreshToken = jwtTokenProvider.generateRefreshToken(saved);

        // Derive the display name: prefer preferredName, fall back to firstName+lastName
        String displayName = (preferredName != null && !preferredName.isEmpty())
                ? preferredName
                : (saved.getFirstName() + " " + saved.getLastName()).trim();

        return OAuth2AuthResponse.builder()
                .userId(saved.getId())
                .email(saved.getEmail())
                .name(displayName)
                .provider("email")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(true)
                .expiresIn(jwtExpirationMs / 1000)
                .build();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "GUEST";
        }
        String upper = role.trim().toUpperCase(Locale.ROOT);
        return "HOST".equals(upper) ? "HOST" : "GUEST";
    }

    private String buildEmailBody(String code) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto\">"
                + "<h2 style=\"color:#111\">Verify your email</h2>"
                + "<p style=\"color:#444;font-size:14px\">Use the code below to finish creating your Travolish account. "
                + "It expires in " + CODE_TTL_MINUTES + " minutes.</p>"
                + "<div style=\"font-size:32px;font-weight:bold;letter-spacing:8px;color:#111;"
                + "background:#f4f4f5;padding:16px;text-align:center;border-radius:12px;margin:16px 0\">"
                + code + "</div>"
                + "<p style=\"color:#888;font-size:12px\">If you didn't request this, you can safely ignore this email.</p>"
                + "</div>";
    }
}
