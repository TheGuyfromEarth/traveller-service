package com.travolish.traveller.auth.service.impl;

import com.travolish.traveller.auth.dto.GoogleOAuth2Request;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;
import com.travolish.traveller.auth.service.GoogleOAuth2Service;
import com.travolish.traveller.auth.util.JwtTokenProvider;
import com.travolish.traveller.notifications.whatsapp.WhatsAppOnboardingHints;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class GoogleOAuth2ServiceImpl implements GoogleOAuth2Service {

    private static final List<String> GOOGLE_ISSUERS = List.of(
            "accounts.google.com",
            "https://accounts.google.com"
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    @Qualifier("googleIdTokenDecoder")
    private JwtDecoder googleIdTokenDecoder;

    @Autowired
    private WhatsAppOnboardingHints whatsAppOnboardingHints;

    @Value("${google.oauth2.client-id}")
    private String googleClientId;

    @Value("${google.oauth2.verify-signature:true}")
    private boolean verifySignature;

    @Value("${app.admin-emails:}")
    private String adminEmailsCsv;

    @Override
    public GoogleOAuth2Request.GoogleUserData getUserInfoFromIdToken(String idToken) {
        log.info("Extracting user info from Google ID token");
        Jwt jwt = decodeAndVerify(idToken);

        GoogleOAuth2Request.GoogleUserData userData = new GoogleOAuth2Request.GoogleUserData();
        userData.setSub(jwt.getSubject());
        userData.setEmail(jwt.getClaimAsString("email"));
        userData.setName(jwt.getClaimAsString("name"));
        userData.setGivenName(jwt.getClaimAsString("given_name"));
        userData.setFamilyName(jwt.getClaimAsString("family_name"));
        userData.setPicture(jwt.getClaimAsString("picture"));
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        userData.setEmailVerified(Boolean.TRUE.equals(emailVerified));

        if (userData.getEmail() == null || userData.getEmail().isBlank()) {
            throw new SecurityException("Google ID token missing email");
        }
        if (userData.getSub() == null || userData.getSub().isBlank()) {
            throw new SecurityException("Google ID token missing subject");
        }
        if (!Boolean.TRUE.equals(userData.getEmailVerified())) {
            throw new SecurityException("Google email is not verified");
        }

        log.debug("Extracted Google user: {}", userData.getEmail());
        return userData;
    }

    @Override
    @Transactional
    public OAuth2AuthResponse handleGoogleCallback(GoogleOAuth2Request request) {
        log.info("Handling Google OAuth2 callback");
        try {
            GoogleOAuth2Request.GoogleUserData userInfo = getUserInfoFromIdToken(request.getIdToken());

            Optional<User> existingUser = userRepository.findByProviderAndProviderId("google", userInfo.getSub());

            User user;
            boolean isNewUser = false;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setEmail(userInfo.getEmail());
                user.setFirstName(userInfo.getGivenName());
                user.setLastName(userInfo.getFamilyName());
                user.setImageKey(userInfo.getPicture());
                user.setProvider("google");
                user.setProviderId(userInfo.getSub());
                log.debug("Updated existing Google user: {}", user.getId());
            } else {
                user = new User();
                user.setEmail(userInfo.getEmail());
                user.setFirstName(userInfo.getGivenName());
                user.setLastName(userInfo.getFamilyName());
                user.setImageKey(userInfo.getPicture());
                user.setProvider("google");
                user.setProviderId(userInfo.getSub());
                isNewUser = true;
                log.info("Created new Google user: {}", userInfo.getEmail());
            }

            if (adminEmailsCsv != null && !adminEmailsCsv.isBlank()) {
                boolean isAdminEmail = java.util.Arrays.stream(adminEmailsCsv.split(","))
                        .map(String::trim)
                        .anyMatch(e -> e.equalsIgnoreCase(userInfo.getEmail()));
                if (isAdminEmail && !"ADMIN".equalsIgnoreCase(user.getRole())) {
                    user.setRole("ADMIN");
                    log.info("Promoted {} to ADMIN via admin-emails whitelist", userInfo.getEmail());
                }
            }

            userRepository.save(user);

            String jwtToken = jwtTokenProvider.generateToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            log.info("Successfully handled Google OAuth2 callback for user: {}", user.getId());
            return OAuth2AuthResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .provider("google")
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .profileImageUrl(user.getImageKey())
                    .isNewUser(isNewUser)
                    .expiresIn(3600L)
                    .whatsAppOnboardingRequired(whatsAppOnboardingHints.isOnboardingRequired(user))
                    .build();
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error handling Google OAuth2 callback", e);
            throw new RuntimeException("Google OAuth2 callback failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyIdToken(String idToken) {
        try {
            decodeAndVerify(idToken);
            return true;
        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateIdToken(String idToken) {
        return verifyIdToken(idToken);
    }

    private Jwt decodeAndVerify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new SecurityException("Google ID token is blank");
        }

        if (!verifySignature) {
            log.warn("google.oauth2.verify-signature=false — skipping cryptographic verification");
            return decodeUnverifiedLocalOnly(idToken);
        }

        Jwt jwt;
        try {
            jwt = googleIdTokenDecoder.decode(idToken);
        } catch (JwtException e) {
            throw new SecurityException("Invalid Google ID token signature: " + e.getMessage(), e);
        }

        List<String> audience = jwt.getAudience();
        if (audience == null || audience.stream().noneMatch(googleClientId::equals)) {
            throw new SecurityException("Google ID token audience mismatch");
        }

        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        if (issuer == null || GOOGLE_ISSUERS.stream().noneMatch(issuer::equals)) {
            throw new SecurityException("Google ID token issuer mismatch: " + issuer);
        }

        return jwt;
    }

    /** Local/dev only when verify-signature is explicitly disabled. */
    private Jwt decodeUnverifiedLocalOnly(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new SecurityException("Invalid token format");
        }
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson);
            java.time.Instant now = java.time.Instant.now();
            java.util.Map<String, Object> claims = new java.util.HashMap<>();
            claims.put("sub", node.path("sub").asText(null));
            claims.put("email", node.path("email").asText(null));
            claims.put("email_verified", node.path("email_verified").asBoolean(false));
            claims.put("name", node.path("name").asText(null));
            claims.put("given_name", node.path("given_name").asText(null));
            claims.put("family_name", node.path("family_name").asText(null));
            claims.put("picture", node.path("picture").asText(null));
            if (node.has("iss")) {
                claims.put("iss", node.get("iss").asText());
            }
            if (node.has("aud")) {
                if (node.get("aud").isArray()) {
                    java.util.List<String> aud = new java.util.ArrayList<>();
                    node.get("aud").forEach(a -> aud.add(a.asText()));
                    claims.put("aud", aud);
                } else {
                    claims.put("aud", List.of(node.get("aud").asText()));
                }
            }
            return new Jwt(idToken, now, now.plusSeconds(3600), java.util.Map.of("alg", "none"), claims);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Failed to decode unverified Google ID token", e);
        }
    }
}
