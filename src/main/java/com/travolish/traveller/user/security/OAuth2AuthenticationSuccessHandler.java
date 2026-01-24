package com.travolish.traveller.user.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User oauth2User) {
                // Extract provider name dynamically (google, facebook, github, etc.)
                String provider = extractProvider(authentication);
                Map<String, Object> attributes = oauth2User.getAttributes();

                // Extract provider-specific ID
                String providerId = extractProviderId(provider, attributes);
                
                // Extract email - consistent across providers
                String email = (String) attributes.get("email");
                
                // Extract name fields based on provider
                String firstName = extractFirstName(provider, attributes);
                String lastName = extractLastName(provider, attributes);

                // Find existing user by email or create new one
                User user = userRepository.findByEmail(email).orElse(null);

                if (user == null) {
                    log.info("Creating new user from OAuth2: email={}, provider={}", email, provider);
                    user = User.builder()
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .provider(provider)
                            .providerId(providerId)
                            .build();
                    userRepository.save(user);
                } else {
                    // Update provider info if missing
                    boolean changed = false;
                    if (user.getProvider() == null) {
                        user.setProvider(provider);
                        changed = true;
                    }
                    if (user.getProviderId() == null && providerId != null) {
                        user.setProviderId(providerId);
                        changed = true;
                    }
                    if (changed) {
                        log.info("Updating user OAuth2 info: email={}, provider={}", email, provider);
                        userRepository.save(user);
                    } else {
                        log.debug("User already exists with OAuth2 info: email={}", email);
                    }
                }

                // Send JSON response with user info
                sendJsonResponse(response, user);
            } else {
                super.onAuthenticationSuccess(request, response, authentication);
            }
        } catch (Exception e) {
            log.error("Error in OAuth2 authentication success handler", e);
            sendErrorResponse(response, "Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Extract provider name dynamically from authentication token
     */
    private String extractProvider(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId();
        }
        return "unknown";
    }

    /**
     * Extract provider-specific ID based on provider type
     * Google: 'sub', Facebook: 'id'
     */
    private String extractProviderId(String provider, Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "facebook" -> attributes.get("id") != null ? attributes.get("id").toString() : null;
            case "google", "github" -> attributes.get("sub") != null ? attributes.get("sub").toString() : null;
            default -> null;
        };
    }

    /**
     * Extract first name based on provider type
     * Google: 'given_name', Facebook: 'first_name'
     */
    private String extractFirstName(String provider, Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "facebook" -> (String) attributes.get("first_name");
            case "google", "github" -> (String) attributes.get("given_name");
            default -> (String) attributes.get("name");
        };
    }

    /**
     * Extract last name based on provider type
     * Google: 'family_name', Facebook: 'last_name'
     */
    private String extractLastName(String provider, Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "facebook" -> (String) attributes.get("last_name");
            case "google", "github" -> (String) attributes.get("family_name");
            default -> null;
        };
    }

    /**
     * Send JSON response with user info using ObjectMapper for safe serialization
     */
    private void sendJsonResponse(HttpServletResponse response, User user) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("id", user.getId());
        responseData.put("email", user.getEmail() != null ? user.getEmail() : "");
        responseData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        responseData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        responseData.put("provider", user.getProvider());
        
        response.getWriter().write(objectMapper.writeValueAsString(responseData));
        response.getWriter().flush();
        
        log.info("OAuth2 authentication successful: userId={}, provider={}", user.getId(), user.getProvider());
    }

    /**
     * Send error JSON response
     */
    private void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", errorMessage);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorData));
        response.getWriter().flush();
    }
}
