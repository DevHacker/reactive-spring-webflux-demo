package com.gtechsolutions.movieinfoservice.controller;

import com.gtechsolutions.movieinfoservice.domain.MovieInfo;
import com.gtechsolutions.movieinfoservice.exception.MovieInfoNotFoundException;
import com.gtechsolutions.movieinfoservice.service.MoviesInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/movieinfos")
@RequiredArgsConstructor
@Slf4j
public class MovieInfoController {

    private final MoviesInfoService movieInfoService;

    @PostMapping
    public Mono<ResponseEntity<MovieInfo>> addMovieInfo(@RequestBody @Valid MovieInfo movieInfo){
        return movieInfoService.addMovieInfo(movieInfo)
                .map(savedMovieInfo -> ResponseEntity.status(HttpStatus.CREATED).body(savedMovieInfo))
                .log();

    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<MovieInfo>> getMovieInfo(@PathVariable String id){
        return movieInfoService.getMoviesInfoById(id).log()
                .map(ResponseEntity::ok)
                .onErrorResume(MovieInfoNotFoundException.class, ex -> Mono.just(ResponseEntity.notFound().build()));
    }

    /*@GetMapping("/movie/{id}")
    public Mono<ResponseEntity<MovieInfo>> getMovieInfoByMovieId(@PathVariable Long id){
        return movieInfoService.getMoviesInfoByMovieId(id).log()
                .map(ResponseEntity::ok)
                .onErrorResume(MovieInfoNotFoundException.class, ex -> Mono.just(ResponseEntity.notFound().build()));
    }*/

    @PutMapping("/{id}")
    public Mono<ResponseEntity<?>> updateMovieInfo(@PathVariable String id, @RequestBody MovieInfo movieInfo){
        return movieInfoService.updateMovieInfo(id, movieInfo).log()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .onErrorResume(MovieInfoNotFoundException.class,
                        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage())))
                .onErrorResume(ex -> {
                    log.error("Error occurred while updating movie info with id: {}", id, ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred while updating movie info with id: " + id));
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteMovieInfo(@PathVariable String id){
        return movieInfoService.deleteMovieInfo(id).log()
                .map(message -> ResponseEntity.noContent().<String>build())
                .onErrorResume(MovieInfoNotFoundException.class,
                        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage())))
                .onErrorResume(ex -> {
                    log.error("Error occurred while deleting movie info with id: {}", id, ex);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred while deleting movie info with id: " + id));
                });
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<MovieInfo>>> getMoviesInfoByYear(@RequestParam(required = false) Integer year){
        return Mono.justOrEmpty(year)
                .map(movieInfoService::getMoviesInfoByYear)
                .switchIfEmpty(Mono.fromSupplier(movieInfoService::getAllMoviesInfo))
                .map(ResponseEntity::ok);
    }
}
