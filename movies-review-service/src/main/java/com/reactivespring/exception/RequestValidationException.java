package com.reactivespring.exception;

import java.util.Map;

public class RequestValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public RequestValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
