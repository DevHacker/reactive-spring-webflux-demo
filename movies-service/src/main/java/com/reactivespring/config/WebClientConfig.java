package com.reactivespring.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {
    @Value("${rest-client.movies-info.base-url}")
    private String movieInfoBaseUrl;

    @Value("${rest-client.reviews.base-url}")
    private String movieReviewsBaseUrl;

    @Value("${app.api-key-version}")
    private String apiKeyVersion;

    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("app-connection-pool")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .evictInBackground(Duration.ofSeconds(60))
                .fifo()
                .build();
    }

    @Bean
    public WebClient webClient(
            WebClient.Builder builder,
            ConnectionProvider connectionProvider) {

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
                .responseTimeout(Duration.ofSeconds(2))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)))
                .compress(true);

        return builder
                //.baseUrl(movieInfoBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-VERSION", apiKeyVersion)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                //.filter(bearerTokenPropagation()) // before logging, so logs see the final request
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * Forwards the bearer token of the current authenticated caller
     * (JWT or opaque token) to the downstream service.
     */
    /*private ExchangeFilterFunction bearerTokenPropagation() {
        return (request, next) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof AbstractOAuth2TokenAuthenticationToken<?> tokenAuth) {
                ClientRequest authorized = ClientRequest.from(request)
                        .headers(h -> h.setBearerAuth(tokenAuth.getToken().getTokenValue()))
                        .build();
                return next.exchange(authorized);
            }
            return next.exchange(request);
        };
    }*/

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            // Log method and URL only, never headers (Authorization / X-API-Key)
            log.info("--> {} {}", req.method(), req.url());
            return Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(res -> {
            log.info("<-- {}", res.statusCode());
            return Mono.just(res);
        });
    }
}

