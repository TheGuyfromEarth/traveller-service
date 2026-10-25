package com.travolish.traveller.review.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidReviewException extends RuntimeException {
    public InvalidReviewException(String message) {
        super(message);
    }

    public InvalidReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
