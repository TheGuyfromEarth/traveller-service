package com.travolish.traveller.auth.controller;

import com.travolish.traveller.auth.dto.AppleOAuth2Request;
import com.travolish.traveller.auth.dto.FacebookOAuth2Request;
import com.travolish.traveller.auth.dto.GoogleOAuth2Request;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;
import com.travolish.traveller.auth.service.AppleOAuth2Service;
import com.travolish.traveller.auth.service.FacebookOAuth2Service;
import com.travolish.traveller.auth.service.GoogleOAuth2Service;
import com.travolish.traveller.auth.util.JwtTokenProvider;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class OAuth2AuthController {

    @Autowired
    private AppleOAuth2Service appleOAuth2Service;

    @Autowired
    private FacebookOAuth2Service facebookOAuth2Service;

    @Autowired
    private GoogleOAuth2Service googleOAuth2Service;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    /**
     * Apple OAuth2 Callback
     * POST /api/auth/apple/callback
     * 
     * Handles Apple Sign In authentication callback
     * 
     * @param request AppleOAuth2Request containing authorization code and user data
     * @return OAuth2AuthResponse with access token and user info
     */
    @PostMapping("/apple/callback")
    public ResponseEntity<?> appleCallback(@Valid @RequestBody AppleOAuth2Request request) {
        log.info("Received Apple OAuth2 callback request");
        try {
            if (request.getCode() == null || request.getCode().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new ErrorResponse("Authorization code is required"));
            }

            OAuth2AuthResponse response = appleOAuth2Service.handleAppleCallback(request);
            log.info("Apple authentication successful for user: {}", response.getUserId());
            
            return ResponseEntity
                    .ok()
                    .body(response);
        } catch (SecurityException e) {
            log.warn("Apple authentication security error: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing Apple OAuth2 callback", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Apple authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Facebook OAuth2 Callback
     * POST /api/auth/facebook/callback
     * 
     * Handles Facebook authentication callback
     * 
     * @param request FacebookOAuth2Request containing access token and user ID
     * @return OAuth2AuthResponse with JWT token and user info
     */
    @PostMapping("/facebook/callback")
    public ResponseEntity<?> facebookCallback(@Valid @RequestBody FacebookOAuth2Request request) {
        log.info("Received Facebook OAuth2 callback request");
        try {
            if (request.getAccessToken() == null || request.getAccessToken().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new ErrorResponse("Access token is required"));
            }

            if (request.getUserId() == null || request.getUserId().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new ErrorResponse("User ID is required"));
            }

            OAuth2AuthResponse response = facebookOAuth2Service.handleFacebookCallback(request);
            log.info("Facebook authentication successful for user: {}", response.getUserId());
            
            return ResponseEntity
                    .ok()
                    .body(response);
        } catch (SecurityException e) {
            log.warn("Facebook authentication security error: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing Facebook OAuth2 callback", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Facebook authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Google OAuth2 Callback
     * POST /api/auth/google/callback
     * 
     * Handles Google authentication callback
     * 
     * @param request GoogleOAuth2Request containing ID token
     * @return OAuth2AuthResponse with JWT token and user info
     */
    @PostMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@Valid @RequestBody GoogleOAuth2Request request) {
        log.info("Received Google OAuth2 callback request");
        try {
            if (request.getIdToken() == null || request.getIdToken().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(new ErrorResponse("ID token is required"));
            }

            OAuth2AuthResponse response = googleOAuth2Service.handleGoogleCallback(request);
            log.info("Google authentication successful for user: {}", response.getUserId());
            
            return ResponseEntity
                    .ok()
                    .body(response);
        } catch (SecurityException e) {
            log.warn("Google authentication security error: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing Google OAuth2 callback", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Google authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Token Refresh
     * POST /api/auth/refresh
     *
     * Exchanges a valid refresh token for a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("refreshToken is required"));
        }
        try {
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid or expired refresh token"));
            }
            Claims claims = jwtTokenProvider.extractClaims(refreshToken);
            if (!"refresh".equals(claims.get("type"))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Not a refresh token"));
            }
            Long userId = Long.parseLong(claims.getSubject());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String newAccessToken = jwtTokenProvider.generateToken(user);
            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "expiresIn", 3600
            ));
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Token refresh failed"));
        }
    }

    /**
     * Revoke Apple Authentication
     * POST /api/auth/apple/revoke
     * 
     * Revokes Apple authentication and refresh token
     * 
     * @param refreshToken Apple refresh token to revoke
     * @return success/failure response
     */
    @PostMapping("/apple/revoke")
    public ResponseEntity<?> revokeApple(@RequestParam String refreshToken) {
        log.info("Revoking Apple authentication token");
        try {
            boolean revoked = appleOAuth2Service.revokeRefreshToken(refreshToken);
            
            if (revoked) {
                log.info("Apple token revoked successfully");
                return ResponseEntity
                        .ok()
                        .body(new SuccessResponse("Token revoked successfully"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Failed to revoke token"));
            }
        } catch (Exception e) {
            log.error("Error revoking Apple token", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Token revocation failed: " + e.getMessage()));
        }
    }

    /**
     * Revoke Facebook Authentication
     * POST /api/auth/facebook/revoke
     * 
     * Revokes Facebook authentication access token
     * 
     * @param accessToken Facebook access token to revoke
     * @return success/failure response
     */
    @PostMapping("/facebook/revoke")
    public ResponseEntity<?> revokeFacebook(@RequestParam String accessToken) {
        log.info("Revoking Facebook authentication token");
        try {
            boolean revoked = facebookOAuth2Service.revokeAccessToken(accessToken);
            
            if (revoked) {
                log.info("Facebook token revoked successfully");
                return ResponseEntity
                        .ok()
                        .body(new SuccessResponse("Token revoked successfully"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Failed to revoke token"));
            }
        } catch (Exception e) {
            log.error("Error revoking Facebook token", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Token revocation failed: " + e.getMessage()));
        }
    }

    /**
     * Validate Apple ID Token
     * POST /api/auth/apple/validate
     * 
     * Validates an Apple ID token without creating a session
     * 
     * @param idToken Apple ID token to validate
     * @return validation result
     */
    @PostMapping("/apple/validate")
    public ResponseEntity<?> validateAppleToken(@RequestParam String idToken) {
        log.info("Validating Apple ID token");
        try {
            boolean isValid = appleOAuth2Service.verifyIdToken(idToken);
            
            return ResponseEntity
                    .ok()
                    .body(new ValidationResponse(isValid));
        } catch (Exception e) {
            log.error("Error validating Apple ID token", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Validation failed: " + e.getMessage()));
        }
    }

    /**
     * Validate Facebook Access Token
     * POST /api/auth/facebook/validate
     * 
     * Validates a Facebook access token without creating a session
     * 
     * @param accessToken Facebook access token to validate
     * @return validation result
     */
    @PostMapping("/facebook/validate")
    public ResponseEntity<?> validateFacebookToken(@RequestParam String accessToken) {
        log.info("Validating Facebook access token");
        try {
            boolean isValid = facebookOAuth2Service.validateAccessToken(accessToken);
            
            return ResponseEntity
                    .ok()
                    .body(new ValidationResponse(isValid));
        } catch (Exception e) {
            log.error("Error validating Facebook access token", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Validation failed: " + e.getMessage()));
        }
    }

    /**
     * Validate Google ID Token
     * POST /api/auth/google/validate
     * 
     * Validates a Google ID token without creating a session
     * 
     * @param idToken Google ID token to validate
     * @return validation result
     */
    @PostMapping("/google/validate")
    public ResponseEntity<?> validateGoogleToken(@RequestParam String idToken) {
        log.info("Validating Google ID token");
        try {
            boolean isValid = googleOAuth2Service.validateIdToken(idToken);
            
            return ResponseEntity
                    .ok()
                    .body(new ValidationResponse(isValid));
        } catch (Exception e) {
            log.error("Error validating Google ID token", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Validation failed: " + e.getMessage()));
        }
    }

    // Helper Response Classes

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private Long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SuccessResponse {
        private String message;
        private Long timestamp;

        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ValidationResponse {
        private Boolean isValid;
        private Long timestamp;

        public ValidationResponse(Boolean isValid) {
            this.isValid = isValid;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
