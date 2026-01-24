package com.travolish.traveller.user.security;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to the default implementation for loading a user
        org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService delegate =
                new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();

        OAuth2User oauth2User = delegate.loadUser(userRequest);

        // You can normalize attributes here. For now, return a DefaultOAuth2User so the success handler can read attributes.
        Map<String, Object> attributes = oauth2User.getAttributes();

        return new DefaultOAuth2User(oauth2User.getAuthorities(), attributes, "sub");
    }
}
