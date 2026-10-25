package com.travolish.traveller.auth.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * OAuth2 Configuration for Apple and Facebook
 * 
 * Properties required in application.yml:
 * apple:
 *   client-id: ${APPLE_CLIENT_ID}
 *   team-id: ${APPLE_TEAM_ID}
 *   key-id: ${APPLE_KEY_ID}
 *   key-path: ${APPLE_KEY_PATH}
 * 
 * facebook:
 *   app-id: ${FACEBOOK_APP_ID}
 *   app-secret: ${FACEBOOK_APP_SECRET}
 *   redirect-uri: https://api.travolish.com/api/auth/facebook/callback
 */
@Configuration
public class OAuth2Config {

    @Value("${apple.client-id:}")
    private String appleClientId;

    @Value("${apple.team-id:}")
    private String appleTeamId;

    @Value("${apple.key-id:}")
    private String appleKeyId;

    @Value("${facebook.app-id:}")
    private String facebookAppId;

    @Value("${facebook.app-secret:}")
    private String facebookAppSecret;

    /**
     * RestTemplate bean for OAuth2 API calls
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * ObjectMapper bean for JSON serialization/deserialization.
     * JavaTimeModule + WRITE_DATES_AS_TIMESTAMPS=false ensures LocalDateTime /
     * OffsetDateTime fields serialize as ISO-8601 strings, not arrays.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }

    /**
     * Validate that required OAuth2 configuration is present
     */
    public void validateConfiguration() {
        if ((appleClientId == null || appleClientId.isEmpty()) &&
            (facebookAppId == null || facebookAppId.isEmpty())) {
            throw new RuntimeException("At least one OAuth2 provider must be configured (Apple or Facebook)");
        }

        if (appleClientId != null && !appleClientId.isEmpty()) {
            if (appleTeamId == null || appleTeamId.isEmpty()) {
                throw new RuntimeException("Apple Team ID is required when Apple Client ID is configured");
            }
            if (appleKeyId == null || appleKeyId.isEmpty()) {
                throw new RuntimeException("Apple Key ID is required when Apple Client ID is configured");
            }
        }

        if (facebookAppId != null && !facebookAppId.isEmpty()) {
            if (facebookAppSecret == null || facebookAppSecret.isEmpty()) {
                throw new RuntimeException("Facebook App Secret is required when Facebook App ID is configured");
            }
        }
    }
}
