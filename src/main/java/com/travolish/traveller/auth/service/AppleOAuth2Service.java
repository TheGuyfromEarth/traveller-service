package com.travolish.traveller.auth.service;

import com.travolish.traveller.auth.dto.AppleOAuth2Request;
import com.travolish.traveller.auth.dto.AppleTokenResponse;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;

public interface AppleOAuth2Service {
    /**
     * Exchange Apple authorization code for tokens
     */
    AppleTokenResponse exchangeCodeForToken(String code);

    /**
     * Get Apple user info from ID token
     */
    AppleOAuth2Request.AppleUserData getUserInfoFromIdToken(String idToken);

    /**
     * Handle Apple OAuth2 callback
     */
    OAuth2AuthResponse handleAppleCallback(AppleOAuth2Request request);

    /**
     * Verify ID token signature
     */
    boolean verifyIdToken(String idToken);

    /**
     * Revoke Apple refresh token
     */
    boolean revokeRefreshToken(String refreshToken);
}
