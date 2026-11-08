package com.travolish.traveller.auth.exception;

/**
 * Thrown when OAuth2 provider communication fails
 */
public class OAuth2ProviderException extends RuntimeException {
    
    private String provider;
    private int httpStatus;

    public OAuth2ProviderException(String message) {
        super(message);
    }

    public OAuth2ProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public OAuth2ProviderException(String message, String provider) {
        super(message);
        this.provider = provider;
    }

    public OAuth2ProviderException(String message, String provider, int httpStatus) {
        super(message);
        this.provider = provider;
        this.httpStatus = httpStatus;
    }

    public OAuth2ProviderException(String message, String provider, int httpStatus, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.httpStatus = httpStatus;
    }

    public String getProvider() {
        return provider;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
