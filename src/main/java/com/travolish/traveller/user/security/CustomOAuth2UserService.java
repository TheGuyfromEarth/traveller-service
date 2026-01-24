package com.travolish.traveller.user.security;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to the default implementation for loading a user
        org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService delegate =
                new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();

        OAuth2User oauth2User = delegate.loadUser(userRequest);

        // Extract provider to determine name attribute
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String nameAttribute = getNameAttribute(registrationId);
        
        // Normalize attributes and return with provider-specific name attribute
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        log.debug("Loading OAuth2 user from provider: {}, name attribute: {}", registrationId, nameAttribute);

        return new DefaultOAuth2User(oauth2User.getAuthorities(), attributes, nameAttribute);
    }

    /**
     * Get the name attribute for each provider
     * - Google and Apple use 'sub'
     * - Facebook uses 'id'
     */
    private String getNameAttribute(String provider) {
        return switch (provider.toLowerCase()) {
            case "google", "apple" -> "sub";
            case "facebook" -> "id";
            default -> "sub";
        };
    }
}
