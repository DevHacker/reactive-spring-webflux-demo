package com.gtechsolutions.movieinfoservice.service;

import com.gtechsolutions.movieinfoservice.domain.MovieInfo;
import com.gtechsolutions.movieinfoservice.exception.MovieInfoNotFoundException;
import com.gtechsolutions.movieinfoservice.repository.MovieInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MoviesInfoService {
    private final MovieInfoRepository movieInfoRepository;

    public Mono<MovieInfo> addMovieInfo(MovieInfo movieInfo) {
        return movieInfoRepository.save(movieInfo);
    }

    public Flux<MovieInfo> getAllMoviesInfo() {
        return movieInfoRepository.findAll().log();
    }

    public Mono<MovieInfo> getMoviesInfoById(String id) {
        return movieInfoRepository.findById(id)
                .switchIfEmpty(Mono.error(new MovieInfoNotFoundException("Movie info not found for id: " + id)))
                .log();
    }

    /*public Mono<MovieInfo> getMoviesInfoByMovieId(Long movieId) {
        return movieInfoRepository.findByMovieId(movieId).log()
                .switchIfEmpty(Mono.error(() -> new MovieInfoNotFoundException("Movie info not found for movieId: " + movieId)));
    }*/

    public Mono<MovieInfo> updateMovieInfo(String id, MovieInfo movieInfo) {
        return movieInfoRepository.findById(id)
                .switchIfEmpty(Mono.error(new MovieInfoNotFoundException("Movie info not found for id: " + id)))
                .flatMap(existingMovieInfo -> {
                    existingMovieInfo.setName(movieInfo.getName());
                    existingMovieInfo.setReleaseDate(movieInfo.getReleaseDate());
                    existingMovieInfo.setCast(movieInfo.getCast());
                    existingMovieInfo.setYear(movieInfo.getYear());
                    return movieInfoRepository.save(existingMovieInfo);
                });
    }

    public Mono<String> deleteMovieInfo(String id) {
        return movieInfoRepository.findById(id)
                .switchIfEmpty(Mono.error(new MovieInfoNotFoundException("Movie info not found for id: " + id)))
                .flatMap(movieInfo -> movieInfoRepository.delete(movieInfo)
                        .thenReturn("Movie info deleted successfully"));
    }

    public Flux<MovieInfo> getMoviesInfoByYear(int year) {
        return movieInfoRepository.findByYear(year);
    }
}
