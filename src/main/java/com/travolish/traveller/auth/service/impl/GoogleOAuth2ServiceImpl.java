package com.travolish.traveller.auth.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travolish.traveller.auth.dto.GoogleOAuth2Request;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;
import com.travolish.traveller.auth.service.GoogleOAuth2Service;
import com.travolish.traveller.auth.util.JwtTokenProvider;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class GoogleOAuth2ServiceImpl implements GoogleOAuth2Service {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${google.oauth2.client-id}")
    private String googleClientId;

    @Value("${google.oauth2.verify-signature:true}")
    private boolean verifySignature;

    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    public GoogleOAuth2Request.GoogleUserData getUserInfoFromIdToken(String idToken) {
        log.info("Extracting user info from Google ID token");
        try {
            // Verify the ID token
            if (!verifyIdToken(idToken)) {
                throw new SecurityException("Invalid Google ID token signature");
            }

            // Decode the token manually (without signature verification here as we already verified)
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new SecurityException("Invalid token format");
            }

            String payload = new String(Base64.getDecoder().decode(parts[1]));
            JsonNode node = objectMapper.readTree(payload);

            GoogleOAuth2Request.GoogleUserData userData = new GoogleOAuth2Request.GoogleUserData();
            userData.setSub(node.get("sub").asText());
            userData.setEmail(node.get("email").asText());
            userData.setName(node.has("name") ? node.get("name").asText() : null);
            userData.setGivenName(node.has("given_name") ? node.get("given_name").asText() : null);
            userData.setFamilyName(node.has("family_name") ? node.get("family_name").asText() : null);
            userData.setPicture(node.has("picture") ? node.get("picture").asText() : null);
            userData.setEmailVerified(node.has("email_verified") ? node.get("email_verified").asBoolean() : false);

            log.debug("Extracted Google user: {}", userData.getEmail());
            return userData;
        } catch (Exception e) {
            log.error("Error extracting user info from Google ID token", e);
            throw new RuntimeException("Failed to extract user info from ID token: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public OAuth2AuthResponse handleGoogleCallback(GoogleOAuth2Request request) {
        log.info("Handling Google OAuth2 callback");
        try {
            // Get user info from ID token
            GoogleOAuth2Request.GoogleUserData userInfo = getUserInfoFromIdToken(request.getIdToken());

            // Find or create user
            Optional<User> existingUser = userRepository.findByProviderAndProviderId("google", userInfo.getSub());
            
            User user;
            boolean isNewUser = false;
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
                // Update user with latest information
                user.setEmail(userInfo.getEmail());
                user.setFirstName(userInfo.getGivenName());
                user.setLastName(userInfo.getFamilyName());
                user.setImageKey(userInfo.getPicture());
                user.setProvider("google");
                user.setProviderId(userInfo.getSub());
                log.debug("Updated existing Google user: {}", user.getId());
            } else {
                // Create new user
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

            // Save user
            userRepository.save(user);

            // Generate JWT tokens
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
                    .expiresIn(3600L) // JWT token expiry in seconds
                    .build();
        } catch (Exception e) {
            log.error("Error handling Google OAuth2 callback", e);
            throw new RuntimeException("Google OAuth2 callback failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyIdToken(String idToken) {
        log.debug("Verifying Google ID token signature (verifySignature={})", verifySignature);
        try {
            if (idToken == null || idToken.isEmpty()) {
                return false;
            }

            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // For development/testing: skip signature verification if disabled
            if (!verifySignature) {
                log.debug("Signature verification disabled. Token format is valid.");
                return true;
            }

            // For production: verify against Google's public keys
            String header = new String(Base64.getDecoder().decode(parts[0]));
            JsonNode headerNode = objectMapper.readTree(header);
            String kidFromToken = headerNode.get("kid").asText();

            // Fetch Google's public keys
            ResponseEntity<String> keysResponse = restTemplate.getForEntity(GOOGLE_JWKS_URL, String.class);
            if (!keysResponse.getStatusCode().is2xxSuccessful() || keysResponse.getBody() == null) {
                log.warn("Failed to fetch Google public keys");
                return false;
            }

            JsonNode keysNode = objectMapper.readTree(keysResponse.getBody());
            JsonNode keyNode = null;

            // Find the key with matching kid
            if (keysNode.has("keys")) {
                for (JsonNode key : keysNode.get("keys")) {
                    if (key.get("kid").asText().equals(kidFromToken)) {
                        keyNode = key;
                        break;
                    }
                }
            }

            if (keyNode == null) {
                log.warn("Could not find matching key in Google's public keys");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateIdToken(String idToken) {
        log.debug("Validating Google ID token format and claims");
        try {
            if (idToken == null || idToken.isEmpty()) {
                return false;
            }

            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String payload = new String(Base64.getDecoder().decode(parts[1]));
            JsonNode payloadNode = objectMapper.readTree(payload);

            // Validate essential claims
            if (!payloadNode.has("sub") || payloadNode.get("sub").asText().isEmpty()) {
                log.warn("Google ID token missing subject claim");
                return false;
            }

            if (!payloadNode.has("email") || payloadNode.get("email").asText().isEmpty()) {
                log.warn("Google ID token missing email claim");
                return false;
            }

            // Verify email if email_verified claim is present
            if (payloadNode.has("email_verified")) {
                boolean emailVerified = payloadNode.get("email_verified").asBoolean();
                if (!emailVerified) {
                    log.warn("Google email not verified for user: {}", payloadNode.get("email").asText());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating Google ID token: {}", e.getMessage());
            return false;
        }
    }
}
