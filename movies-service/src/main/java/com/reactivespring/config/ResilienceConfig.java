package com.reactivespring.config;

import com.reactivespring.exception.ReviewNotFoundException;
import com.reactivespring.exception.ReviewsClientException;
import com.reactivespring.exception.ReviewsServerException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreaker reviewsServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Ouvre le circuit si 50% des requêtes échouent
                .waitDurationInOpenState(Duration.ofSeconds(2)) // Reste ouvert 2s avant de tester à nouveau
                .slidingWindowSize(10)
                
                // CRUCIAL : N'ouvre PAS le circuit si c'est une erreur 4xx (ex: mauvaise saisie utilisateur)
                .ignoreExceptions(ReviewNotFoundException.class, ReviewsClientException.class)
                
                // Comptabilise ces exceptions comme des échecs (Erreurs 5xx, Timeouts, Réseau)
                .recordExceptions(ReviewsServerException.class, TimeoutException.class)
                .build();

        return registry.circuitBreaker("reviewService", config);
    }
}
