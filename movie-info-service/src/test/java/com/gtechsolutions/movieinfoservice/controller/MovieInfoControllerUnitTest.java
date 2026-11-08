package com.gtechsolutions.movieinfoservice.controller;

import com.gtechsolutions.movieinfoservice.domain.MovieInfo;
import com.gtechsolutions.movieinfoservice.exception.MovieInfoNotFoundException;
import com.gtechsolutions.movieinfoservice.service.MoviesInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@WebFluxTest(controllers = MovieInfoController.class)
@AutoConfigureWebTestClient
class MovieInfoControllerUnitTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private MoviesInfoService moviesInfoServiceMock;

    static String MOVIE_INFO_URL = "/v1/movieinfos";

    @Test
    void addMovieInfo() {
        var movieInfo = new MovieInfo(null, "Batman Begins", 2005, List.of("Christian Bale", "Michael Caine"), LocalDate.parse("2005-06-15"));

        when(moviesInfoServiceMock.addMovieInfo(isA(MovieInfo.class)))
                .thenReturn(Mono.just(new MovieInfo("mockId", "Batman Begins", 2005, List.of("Christian Bale", "Michael Caine"), LocalDate.parse("2005-06-15"))));

        //when
        webTestClient.post()
                .uri(MOVIE_INFO_URL)
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(MovieInfo.class)
                .consumeWith(movieInfoEntityExchangeResult -> {
                    var savedMovieInfo = movieInfoEntityExchangeResult.getResponseBody();
                    assertNotNull(savedMovieInfo);
                    assertEquals("mockId", savedMovieInfo.getMovieInfoId());
                    assertEquals("Batman Begins", savedMovieInfo.getName());
                });
    }

    @Test
    void addMovieInfo_validation() {
        var movieInfo = new MovieInfo(null, "", -2005, List.of(""), LocalDate.parse("2005-06-15"));

        //when
        webTestClient.post()
                .uri(MOVIE_INFO_URL)
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getAllMovieInfo() {

        var moviesInfo = List.of(
                new MovieInfo(null, "Batman Begins", 2005, List.of("Christian Bale", "Michael Caine"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight", 2008, List.of("Christian Bale", "Heath Ledger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("12", "Dark Knight Rises", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20"))
        );

        when(moviesInfoServiceMock.getAllMoviesInfo()).thenReturn(Flux.fromIterable(moviesInfo));

        webTestClient.get()
                .uri(MOVIE_INFO_URL)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(3);
    }

    @Test
    void getMoviesInfoByYear() {
        var year = 2005;
        var moviesInfo = List.of(
                new MovieInfo("1", "Batman Begins", year, List.of("Christian Bale", "Michael Caine"), LocalDate.parse("2005-06-15"))
        );

        when(moviesInfoServiceMock.getMoviesInfoByYear(year)).thenReturn(Flux.fromIterable(moviesInfo));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(MOVIE_INFO_URL).queryParam("year", year).build())
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(1);

        verify(moviesInfoServiceMock, never()).getAllMoviesInfo();
    }

    @Test
    void getMovieInfo() {
        var movieInfoId = "12";
        var movieInfo = new MovieInfo("12", "Dark Knight Rises", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20"));

        when(moviesInfoServiceMock.getMoviesInfoById(movieInfoId)).thenReturn(Mono.just(movieInfo));

        webTestClient.get()
                .uri(MOVIE_INFO_URL + "/" + movieInfoId)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(MovieInfo.class)
                .consumeWith(movieInfoEntityExchangeResult -> {
                    var movieInfoEntityExchangeResultResponseBody = movieInfoEntityExchangeResult.getResponseBody();
                    assertNotNull(movieInfoEntityExchangeResultResponseBody);
                    assertNotNull(movieInfoEntityExchangeResultResponseBody.getMovieInfoId());
                    assertEquals("12", movieInfoEntityExchangeResultResponseBody.getMovieInfoId());
                    assertEquals("2012-07-20", movieInfoEntityExchangeResultResponseBody.getReleaseDate().toString());
                });
    }

    @Test
    void getMovieInfo_notFound() {
        var movieInfoId = "unknownId";

        when(moviesInfoServiceMock.getMoviesInfoById(movieInfoId))
                .thenReturn(Mono.error(new MovieInfoNotFoundException("Movie info not found for id: " + movieInfoId)));

        webTestClient.get()
                .uri(MOVIE_INFO_URL + "/{id}", movieInfoId)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void updateMovieInfo() {
        var movieInfoToUpDate = new MovieInfo("12", "Dark Knight Rises1", 2012, List.of("Christian Bale", "Tom Hardy", "Fidele DONVIDE"), LocalDate.parse("2012-07-20"));

        when(moviesInfoServiceMock.updateMovieInfo(eq(movieInfoToUpDate.getMovieInfoId()), isA(MovieInfo.class))).thenReturn(Mono.just(movieInfoToUpDate));


        //when
        webTestClient.put()
                .uri(MOVIE_INFO_URL + "/{id}", movieInfoToUpDate.getMovieInfoId())
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
    }

    @Test
    void updateMovieInfo_notFound() {
        var movieInfoId = "unknownId";
        var movieInfoToUpDate = new MovieInfo(null, "Dark Knight Rises1", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20"));
        when(moviesInfoServiceMock.updateMovieInfo(eq(movieInfoId), isA(MovieInfo.class)))
                .thenReturn(Mono.error(new MovieInfoNotFoundException("Movie info not found for id: " + movieInfoId)));

        //when
        webTestClient.put()
                .uri(MOVIE_INFO_URL + "/{id}", movieInfoId)
                .bodyValue(movieInfoToUpDate)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo("Movie info not found for id: " + movieInfoId);
    }

    @Test
    void updateMovieInfo_serverError() {
        var movieInfoId = "12";
        var movieInfoToUpDate = new MovieInfo(null, "Dark Knight Rises1", 2012, List.of("Christian Bale", "Tom Hardy"), LocalDate.parse("2012-07-20"));
        when(moviesInfoServiceMock.updateMovieInfo(eq(movieInfoId), isA(MovieInfo.class)))
                .thenReturn(Mono.error(new RuntimeException("Database unavailable")));

        //when
        webTestClient.put()
                .uri(MOVIE_INFO_URL + "/{id}", movieInfoId)
                .bodyValue(movieInfoToUpDate)
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody(String.class)
                .isEqualTo("An error occurred while updating movie info with id: " + movieInfoId);
    }

    @Test
    void deleteMovieInfo() {
        var movieInfoId = "12";
        when(moviesInfoServiceMock.deleteMovieInfo(movieInfoId)).thenReturn(Mono.just("Movie info deleted successfully"));

        //when
        webTestClient.delete()
                .uri(MOVIE_INFO_URL + "/" + movieInfoId)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    void deleteMovieInfo_notFound() {
        var movieInfoId = "unknownId";
        when(moviesInfoServiceMock.deleteMovieInfo(movieInfoId))
                .thenReturn(Mono.error(new MovieInfoNotFoundException("Movie info not found for id: " + movieInfoId)));

        //when
        webTestClient.delete()
                .uri(MOVIE_INFO_URL + "/{id}", movieInfoId)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo("Movie info not found for id: " + movieInfoId);
    }

    @Test
    void deleteMovieInfo_serverError() {
        var movieInfoId = "12";
        when(moviesInfoServiceMock.deleteMovieInfo(movieInfoId))
                .thenReturn(Mono.error(new RuntimeException("Database unavailable")));

        //when
        webTestClient.delete()
                .uri(MOVIE_INFO_URL + "/{id}", movieInfoId)
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody(String.class)
                .isEqualTo("An error occurred while deleting movie info with id: " + movieInfoId);
    }
}
