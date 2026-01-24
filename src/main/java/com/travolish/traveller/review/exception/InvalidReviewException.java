package com.travolish.traveller.review.exception;

public class InvalidReviewException extends RuntimeException {
    public InvalidReviewException(String message) {
        super(message);
    }

    public InvalidReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
