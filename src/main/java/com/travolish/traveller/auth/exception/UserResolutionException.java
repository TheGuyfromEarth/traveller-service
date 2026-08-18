package com.travolish.traveller.auth.exception;

/**
 * Thrown when user cannot be found or created during OAuth2 authentication
 */
public class UserResolutionException extends RuntimeException {
    
    private String provider;
    private String providerId;

    public UserResolutionException(String message) {
        super(message);
    }

    public UserResolutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserResolutionException(String message, String provider, String providerId) {
        super(message);
        this.provider = provider;
        this.providerId = providerId;
    }

    public UserResolutionException(String message, String provider, String providerId, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.providerId = providerId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }
}
