package com.reactivespring.exception;

public class InvalidReviewException extends RuntimeException {
    public InvalidReviewException(String rawId) {
        super("Invalid review id '%s': a numeric value is expected".formatted(rawId));
    }
}
