package com.reactivespring.exception.handler;

import com.reactivespring.dto.ApiError;
import com.reactivespring.exception.InvalidReviewException;
import com.reactivespring.exception.RequestValidationException;
import com.reactivespring.exception.ReviewNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

@Component
@Order(-2)
@Slf4j
@RequiredArgsConstructor
public class GlobalErrorHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;


    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        ApiError body = toApiError(exchange.getRequest().getPath().value(), ex);

        response.setStatusCode(HttpStatusCode.valueOf(body.status()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(body));
        return response.writeWith(Mono.just(buffer));

    }

    private ApiError toApiError(String path, Throwable ex) {
        return switch (ex) {
            case ReviewNotFoundException e -> build(HttpStatus.NOT_FOUND, e.getMessage(), path, null);

            case InvalidReviewException e -> build(HttpStatus.BAD_REQUEST, e.getMessage(), path, null);

            case RequestValidationException e -> build(HttpStatus.BAD_REQUEST, e.getMessage(), path, e.getErrors());

            // covers ServerWebInputException (bad JSON, missing body), 404 no route, 415, 405...
            case ResponseStatusException e when e.getReason() != null ->
                    build(e.getStatusCode(), e.getReason(), path, null);

            case ResponseStatusException e -> build(e.getStatusCode(), "Request failed", path, null);

            default -> {
                log.error("Unhandled error on {}", path, ex);
                yield build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", path, null);
            }
        };
    }

    private ApiError build(HttpStatusCode status, String message, String path, Map<String, String> errors) {
        String error = status instanceof HttpStatus hs ? hs.getReasonPhrase() : status.toString();
        return new ApiError(Instant.now(), status.value(), error, message, path, errors);
    }
}
