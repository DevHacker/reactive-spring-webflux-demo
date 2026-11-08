package com.reactivespring.controller;

import com.reactivespring.clients.MovieInfoRestClient;
import com.reactivespring.clients.MovieReviewsRestClient;
import com.reactivespring.domain.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {

    private final MovieInfoRestClient movieInfoRestClient;
    private final MovieReviewsRestClient movieReviewsRestClient;

    @PostMapping
    public Mono<Movie> createMovie(Movie movie)
    {
        return Mono.just(movie);
    }

    @GetMapping("/{movieId}")
    public Mono<Movie> retrieveMovieById(@PathVariable String movieId)
    {
       return movieInfoRestClient.retrieveMovieInfoById(movieId)
               .doOnNext(movieInfo -> log.info("Found Movie Info: {}", movieInfo))
               .flatMap(movieInfo -> movieReviewsRestClient.retrieveReviewsByMovieInfoId(movieInfo.getMovieInfoId())
                       .collectList()
                       .map(reviews -> new Movie(movieInfo, reviews)))
               .log();
    }
}
