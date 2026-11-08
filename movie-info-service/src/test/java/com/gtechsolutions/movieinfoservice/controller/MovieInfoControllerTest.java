package com.gtechsolutions.movieinfoservice.controller;

import com.gtechsolutions.movieinfoservice.domain.MovieInfo;
import com.gtechsolutions.movieinfoservice.repository.MovieInfoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class MovieInfoControllerTest {

    @Autowired
    MovieInfoRepository movieInfoRepository;

    @Autowired
    WebTestClient webTestClient;

    static String MOVIE_INFO_URL = "/v1/movieinfos";

    @BeforeEach
    void setUp() {
        var moviesInfo = List.of(
                new MovieInfo(null, "Batman Begins", 2005, List.of("Christian Bale", "Michael Caine"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight", 2008, List.of("Christian Bale", "Heath Ledger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("12", "Dark Knight Rises", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20"))
        );
        movieInfoRepository.saveAll(moviesInfo).blockLast();
    }

    @AfterEach
    void tearDown() {
        movieInfoRepository.deleteAll().block();
    }

    @Test
    void addMovieInfo() {
        //given
        var movieInfo = new MovieInfo(null, "Batman Begins1", 2005, List.of("Christian Bale", "Michael Caine"), LocalDate.parse("2005-06-15"));

        //when
        webTestClient.post()
                .uri(MOVIE_INFO_URL)
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MovieInfo.class)
                .consumeWith(movieInfoEntityExchangeResult -> {
                    var savedMovieInfo = movieInfoEntityExchangeResult.getResponseBody();
                    assert savedMovieInfo != null;
                    assertNotNull(savedMovieInfo.getMovieInfoId());
                });
        //then

    }

    @Test
    void getAllMoviesInfo() {
        //given

        //when
        webTestClient.get()
                .uri(MOVIE_INFO_URL)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(3);
        //then

    }

    @Test
    void getMovieInfoById() {
        //given
        var movieInfoId = "12";

        //when
        webTestClient.get()
                .uri(MOVIE_INFO_URL + "/" + movieInfoId)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(MovieInfo.class)
                .consumeWith(movieInfoEntityExchangeResult -> {
                    var movieInfo = movieInfoEntityExchangeResult.getResponseBody();
                    assertNotNull(movieInfo);
                    assertNotNull(movieInfo.getMovieInfoId());
                    assertEquals("12", movieInfo.getMovieInfoId());
                    assertEquals("2012-07-20", movieInfo.getReleaseDate().toString());
                });
        //then

    }

    @Test
    void updateMovieInfo() {

        //given
        var movieInfoId = "12";
        var movieInfoToUpDate = new MovieInfo(null, "Dark Knight Rises1", 2012, List.of("Christian Bale", "Tom Hardy", "Fidele DONVIDE"), LocalDate.parse("2012-07-20"));

        //when
        webTestClient.put()
                .uri(MOVIE_INFO_URL + "/{id}", movieInfoId)
                .bodyValue(movieInfoToUpDate)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(MovieInfo.class)
                .consumeWith(movieInfoEntityExchangeResult -> {
                    var movieInfo = movieInfoEntityExchangeResult.getResponseBody();
                    assertNotNull(movieInfo);
                    assertNotNull(movieInfo.getMovieInfoId());
                    assertEquals("Dark Knight Rises1", movieInfo.getName());
                });
        //then
    }

    @Test
    void deleteMovieInfo() {

        //given
        var movieInfoId = "12";

        //when
        webTestClient.delete()
                .uri(MOVIE_INFO_URL + "/" + movieInfoId)
                .exchange()
                .expectStatus()
                .isNoContent();
        //then
    }
}
