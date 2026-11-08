package com.reactivespring.clients;

import com.reactivespring.domain.Review;
import com.reactivespring.exception.ReviewNotFoundException;
import com.reactivespring.exception.ReviewsClientException;
import com.reactivespring.exception.ReviewsServerException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MovieReviewsRestClient {
    @Value("${restClient.reviews.baseUrl}")
    private String reviewsBaseUrl;

    private final WebClient webClient;

    private final CircuitBreaker circuitBreaker;

    public MovieReviewsRestClient(WebClient webClient, CircuitBreaker reviewsServiceCircuitBreaker) {
        this.webClient = webClient;
        this.circuitBreaker = reviewsServiceCircuitBreaker;
    }

    public Flux<Review> retrieveReviewsByMovieInfoId(String movieInfoId) {
        String url = reviewsBaseUrl.concat("/{movieInfoId}");
        log.info("Fetching reviews from URL: {}", url);
        return webClient.get()
                .uri(url, movieInfoId)
                .retrieve()
                // 1. Interception fine des status HTTP (onStatus) pour gérer les erreurs 4xx et 5xx
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        log.error("Reviews not found for movieInfoId {}: {}", movieInfoId, clientResponse.statusCode());
                        return Mono.error(new ReviewNotFoundException("Reviews not found for movieInfoId: " + movieInfoId));
                    }
                    return clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("No error body provided")
                            .flatMap(errorMessage -> Mono.error(new ReviewsClientException("Client error occurred while fetching reviews for movieInfoId: " + movieInfoId)));

                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    log.error("Server error occurred while fetching reviews for movieInfoId {}: {}", movieInfoId, clientResponse.statusCode());
                    return clientResponse.bodyToMono(Throwable.class)
                            .switchIfEmpty(Mono.error(new ReviewsServerException("Server error occurred while fetching reviews for movieInfoId: " + movieInfoId)));
                })
                .bodyToFlux(Review.class)
                // 2. Application du Circuit Breaker
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(exception -> {

                    // Cas A : Le Circuit Breaker est OUVERT (Fail-fast automatique)
                    if (exception instanceof CallNotPermittedException) {
                        log.warn("Circuit Breaker ouvert !");
                        return Flux.empty();
                    }

                    // Cas B : Timeouts réseau ou coupures électriques de l'API distante
                    if (this.isTransientNetworkError(exception)) {
                        log.error("Timeout réseau détecté pour movieInfoId {}: {}", movieInfoId, exception.getMessage());
                        return Flux.empty();
                    }

                    // Cas C : C'est notre UpstreamClientException (4xx) ou UpstreamServerException (5xx)
                    // On ne renvoie pas de fallback ici, on laisse l'erreur se propager vers le ControllerAdvice !
                    return Flux.error(exception);
                });
                /*// 2. Add Resiliency (Retry transient network failures up to 3 times before giving up)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(this::isTransientNetworkError)
                        .doBeforeRetry(retrySignal -> log.warn("Retrying due to network issue. Attempt: {}", retrySignal.totalRetries() + 1)))*/

    }

    /**
     * Helper method to filter errors that are safe to retry (timeouts, connection drops).
     */
    private boolean isTransientNetworkError(Throwable throwable) {
        return throwable instanceof ConnectTimeoutException ||
                throwable instanceof ReadTimeoutException ||
                (throwable instanceof WebClientRequestException && throwable.getCause() instanceof java.net.ConnectException);
    }


}
