package com.travolish.traveller.auth.service;

import com.travolish.traveller.auth.dto.FacebookOAuth2Request;
import com.travolish.traveller.auth.dto.FacebookTokenResponse;
import com.travolish.traveller.auth.dto.FacebookUserData;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;

public interface FacebookOAuth2Service {
    /**
     * Exchange Facebook authorization code for tokens
     */
    FacebookTokenResponse exchangeCodeForToken(String code);

    /**
     * Get Facebook user info from access token
     */
    FacebookUserData getUserInfo(String accessToken, String userId);

    /**
     * Handle Facebook OAuth2 callback
     */
    OAuth2AuthResponse handleFacebookCallback(FacebookOAuth2Request request);

    /**
     * Validate Facebook access token
     */
    boolean validateAccessToken(String accessToken);

    /**
     * Revoke Facebook access token
     */
    boolean revokeAccessToken(String accessToken);
}
