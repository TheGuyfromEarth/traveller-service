package com.travolish.traveller.auth.exception;

/**
 * Thrown when OAuth2 token is invalid or verification fails
 */
public class InvalidTokenException extends RuntimeException {
    
    private String provider;
    private String tokenType;

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTokenException(String message, String provider, String tokenType) {
        super(message);
        this.provider = provider;
        this.tokenType = tokenType;
    }

    public InvalidTokenException(String message, String provider, String tokenType, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.tokenType = tokenType;
    }

    public String getProvider() {
        return provider;
    }

    public String getTokenType() {
        return tokenType;
    }
}
