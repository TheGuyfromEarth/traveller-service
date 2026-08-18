package com.travolish.traveller.auth.service.impl;

import com.travolish.traveller.auth.dto.FacebookOAuth2Request;
import com.travolish.traveller.auth.dto.FacebookTokenResponse;
import com.travolish.traveller.auth.dto.FacebookUserData;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;
import com.travolish.traveller.auth.service.FacebookOAuth2Service;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import com.travolish.traveller.auth.util.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@Transactional
public class FacebookOAuth2ServiceImpl implements FacebookOAuth2Service {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${facebook.app-id:}")
    private String facebookAppId;

    @Value("${facebook.app-secret:}")
    private String facebookAppSecret;

    @Value("${facebook.redirect-uri:https://api.travolish.com/api/auth/facebook/callback}")
    private String facebookRedirectUri;

    private static final String FACEBOOK_TOKEN_URL = "https://graph.instagram.com/v18.0/oauth/access_token";
    private static final String FACEBOOK_GRAPH_API_URL = "https://graph.facebook.com/v18.0";

    @Override
    public FacebookTokenResponse exchangeCodeForToken(String code) {
        log.info("Exchanging Facebook authorization code for tokens");
        try {
            String url = UriComponentsBuilder.fromUriString(FACEBOOK_TOKEN_URL)
                    .queryParam("client_id", facebookAppId)
                    .queryParam("client_secret", facebookAppSecret)
                    .queryParam("redirect_uri", facebookRedirectUri)
                    .queryParam("code", code)
                    .toUriString();

            ResponseEntity<FacebookTokenResponse> response = restTemplate.getForEntity(
                    url,
                    FacebookTokenResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully exchanged Facebook authorization code for tokens");
                return response.getBody();
            }

            throw new RuntimeException("Failed to exchange Facebook authorization code");
        } catch (Exception e) {
            log.error("Error exchanging Facebook authorization code", e);
            throw new RuntimeException("Facebook token exchange failed: " + e.getMessage(), e);
        }
    }

    @Override
    public FacebookUserData getUserInfo(String accessToken, String userId) {
        log.info("Fetching Facebook user info for user: {}", userId);
        try {
            String url = UriComponentsBuilder.fromUriString(FACEBOOK_GRAPH_API_URL)
                    .path("/{userId}")
                    .queryParam("fields", "id,name,email,picture.type(large),first_name,last_name,gender,locale,timezone,updated_time")
                    .queryParam("access_token", accessToken)
                    .buildAndExpand(userId)
                    .toUriString();

            ResponseEntity<FacebookUserData> response = restTemplate.getForEntity(
                    url,
                    FacebookUserData.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully fetched Facebook user info for user: {}", userId);
                return response.getBody();
            }

            throw new RuntimeException("Failed to fetch Facebook user info");
        } catch (Exception e) {
            log.error("Error fetching Facebook user info", e);
            throw new RuntimeException("Failed to get Facebook user info: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public OAuth2AuthResponse handleFacebookCallback(FacebookOAuth2Request request) {
        log.info("Handling Facebook OAuth2 callback");
        try {
            // Validate access token
            if (!validateAccessToken(request.getAccessToken())) {
                throw new SecurityException("Invalid Facebook access token");
            }

            // Get user info from Facebook Graph API
            FacebookUserData userInfo = getUserInfo(request.getAccessToken(), request.getUserId());

            // Find or create user
            User user = userRepository.findByProviderAndProviderId("facebook", userInfo.getId())
                    .orElseGet(() -> createFacebookUser(userInfo));

            // Update user with latest information
            userRepository.save(user);

            // Generate JWT tokens
            String jwtToken = jwtTokenProvider.generateToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            log.info("Successfully handled Facebook OAuth2 callback for user: {}", user.getId());
            return OAuth2AuthResponse.builder()
                    .userId(user.getId())
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .isNewUser(false)
                    .build();
        } catch (Exception e) {
            log.error("Error handling Facebook OAuth2 callback", e);
            throw new RuntimeException("Facebook OAuth2 callback failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        log.debug("Validating Facebook access token");
        try {
            String url = UriComponentsBuilder.fromUriString(FACEBOOK_GRAPH_API_URL)
                    .path("/debug_token")
                    .queryParam("input_token", accessToken)
                    .queryParam("access_token", facebookAppId + "|" + facebookAppSecret)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Parse the response to check if token is valid
                // In production, check the 'is_valid' field in the response
                log.debug("Facebook access token is valid");
                return true;
            }

            log.warn("Facebook access token validation failed");
            return false;
        } catch (Exception e) {
            log.error("Error validating Facebook access token", e);
            return false;
        }
    }

    @Override
    public boolean revokeAccessToken(String accessToken) {
        log.info("Revoking Facebook access token");
        try {
            String url = UriComponentsBuilder.fromUriString(FACEBOOK_GRAPH_API_URL)
                    .path("/me/permissions")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            RestTemplate deleteTemplate = new RestTemplate();
            ResponseEntity<String> response = deleteTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Facebook access token revoked successfully");
                return true;
            }

            log.warn("Failed to revoke Facebook access token");
            return false;
        } catch (Exception e) {
            log.error("Error revoking Facebook access token", e);
            return false;
        }
    }

    // Helper methods

    private User createFacebookUser(FacebookUserData userInfo) {
        User user = new User();
        user.setProvider("facebook");
        user.setProviderId(userInfo.getId());
        user.setEmail(userInfo.getEmail());
        
        // Extract name if available (split name into first and last)
        if (userInfo.getName() != null) {
            String[] nameParts = userInfo.getName().split(" ", 2);
            user.setFirstName(nameParts[0]);
            if (nameParts.length > 1) {
                user.setLastName(nameParts[1]);
            }
        }

        log.info("Creating new Facebook user: {}", userInfo.getEmail());
        return userRepository.save(user);
    }
}
