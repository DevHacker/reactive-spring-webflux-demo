package com.gtechsolutions.movieinfoservice.repository;

import com.gtechsolutions.movieinfoservice.domain.MovieInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataMongoTest
@ActiveProfiles("test")
class MovieInfoRepositoryIntegrationTest {

    @Autowired
    MovieInfoRepository movieInfoRepository;

    @BeforeEach
    void setUp(){
        var moviesInfo = List.of(
                new MovieInfo(null, "Batman Begins", 2005, List.of("Christian Bale", "Michael Caine"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight", 2008, List.of("Christian Bale", "Heath Ledger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("12", "Dark Knight Rises", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20"))
        );
        movieInfoRepository.saveAll(moviesInfo).blockLast();
    }

    @AfterEach
    void tearDown(){
        movieInfoRepository.deleteAll().block();
    }


    @Test
    void findAll(){
        //given

        //when
        var moviesInfoFlux = movieInfoRepository.findAll().log();

        //then
        StepVerifier.create(moviesInfoFlux)
                .expectNextCount(3)
                .verifyComplete();

    }

    @Test
    void findById(){
        //given

        //when
        var moviesInfoMono = movieInfoRepository.findById("12").log();

        //then
        StepVerifier.create(moviesInfoMono)
                //.expectNextCount(1)
                .assertNext(movieInfo -> {
                    assertEquals("Dark Knight Rises", movieInfo.getName());
                })
                .verifyComplete();

    }

    @Test
    void saveMovie(){
        //given
        var foundMovie = movieInfoRepository.findById("12")
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(movieInfo ->{
                    movieInfo.setYear(2026);
                    movieInfoRepository.save(movieInfo).block();
                }
                ).log();

        //when

        //then
        StepVerifier.create(foundMovie)
                .assertNext(savedInfo -> {
                    assertEquals(2026, savedInfo.getYear());
                })
                .verifyComplete();
    }


    @Test
    void updateMovie(){
        //given
        MovieInfo movieInfo = new MovieInfo(null, "Batman Begins1", 2005, List.of("Christian Bale", "Michael Caine"), LocalDate.parse("2005-06-15"));

        //when
        var savedMovieInfo = movieInfoRepository.save(movieInfo).log();

        //then
        StepVerifier.create(savedMovieInfo)
                .assertNext(savedInfo -> {
                    assertNotNull(savedInfo.getMovieInfoId());
                    assertEquals("Batman Begins1", savedInfo.getName());

                })
                .verifyComplete();
    }

    @Test
    void deleteMovie(){
        //given

        //when
        movieInfoRepository.deleteById("12").block();
        var moviesInfoFlux = movieInfoRepository.findAll().log();

        //then
        StepVerifier.create(moviesInfoFlux)
                .expectNextCount(2)
                .verifyComplete();

    }
}
