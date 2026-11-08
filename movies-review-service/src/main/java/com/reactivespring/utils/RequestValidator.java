package com.reactivespring.utils;

import com.reactivespring.exception.RequestValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestValidator {

    private final Validator validator;

    public <T> Mono<T> validate(T body) {
        Set<ConstraintViolation<T>> violations = validator.validate(body);
        if (violations.isEmpty()) {
            return Mono.just(body);
        }
        Map<String, String> errors = violations.stream().collect(Collectors.toMap(
            v -> v.getPropertyPath().toString(),
            ConstraintViolation::getMessage,
            (a, b) -> a + "; " + b,
            TreeMap::new));
        return Mono.error(new RequestValidationException(errors));
    }
}
