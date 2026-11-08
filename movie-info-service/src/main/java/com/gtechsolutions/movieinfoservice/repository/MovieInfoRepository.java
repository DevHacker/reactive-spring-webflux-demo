package com.gtechsolutions.movieinfoservice.repository;

import com.gtechsolutions.movieinfoservice.domain.MovieInfo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MovieInfoRepository extends ReactiveMongoRepository<MovieInfo, String> {
    Flux<MovieInfo> findByYear(int year);
    Flux<MovieInfo> findByName(String name);
    //Mono<MovieInfo> findByMovieId(Long movieId);
}
