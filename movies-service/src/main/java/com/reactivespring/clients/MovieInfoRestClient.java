package com.reactivespring.clients;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.exception.MoviesInfoClientException;
import com.reactivespring.exception.MoviesInfoServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieInfoRestClient {
    @Value("${restClient.moviesInfo.baseUrl}")
    private String movieInfoBaseUrl;

    private final WebClient webClient;

    public Mono<MovieInfo> retrieveMovieInfoById(String movieId) {
        String url = movieInfoBaseUrl.concat("/{id}");
        log.info("Fetching movie info from URL: {}", url);
        return webClient.get()
                .uri(url, movieId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        log.error("Movie info not found for movieId {}: {}", movieId, clientResponse.statusCode());
                        return  Mono.error(new MoviesInfoClientException("Movie info not found for movieId: " + movieId, clientResponse.statusCode().value()));
                    }

                    log.error("Client error while fetching movie info for movieId {}: {}", movieId, clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("No error body provided")
                            .flatMap(errorMessage -> Mono.error(new MoviesInfoClientException(errorMessage, clientResponse.statusCode().value())));

                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    log.error("Server error while fetching movie info for movieId {}: {}", movieId, clientResponse.statusCode());
                    return Mono.error(new MoviesInfoServerException("Server error while fetching movie info"));
                })
                .bodyToMono(MovieInfo.class)
                .doOnError(e -> log.error("Error fetching movie info for movieId {}: {}", movieId, e.getMessage()));
    }


}
