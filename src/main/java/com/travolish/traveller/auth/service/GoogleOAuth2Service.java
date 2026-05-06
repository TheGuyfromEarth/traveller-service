package com.travolish.traveller.auth.service;

import com.travolish.traveller.auth.dto.GoogleOAuth2Request;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;

public interface GoogleOAuth2Service {
    /**
     * Get Google user info from ID token
     */
    GoogleOAuth2Request.GoogleUserData getUserInfoFromIdToken(String idToken);

    /**
     * Handle Google OAuth2 callback
     */
    OAuth2AuthResponse handleGoogleCallback(GoogleOAuth2Request request);

    /**
     * Verify ID token signature
     */
    boolean verifyIdToken(String idToken);

    /**
     * Validate ID token format and claims
     */
    boolean validateIdToken(String idToken);
}
