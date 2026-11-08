package com.reactivespring.unit;

import com.reactivespring.domain.Review;
import com.reactivespring.exception.handler.GlobalErrorHandler;
import com.reactivespring.handler.ReviewHandler;
import com.reactivespring.repository.ReviewRepository;
import com.reactivespring.router.ReviewRouter;
import com.reactivespring.utils.RequestValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@WebFluxTest
@ContextConfiguration(classes = {ReviewRouter.class, ReviewHandler.class, GlobalErrorHandler.class})
@AutoConfigureWebTestClient
public class ReviewsUnitTest {

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private RequestValidator requestValidator;

    @Autowired
    private WebTestClient webTestClient;

    static String REVIEW_INFO_URL = "/api/reviews";

    @Test
    void addReview() {
        var review = new Review(null, 1L, "Batman Begins", 9.0);

        when(requestValidator.validate(isA(Review.class))).thenReturn(Mono.just(review));
        when(reviewRepository.save(isA(Review.class))).thenReturn(Mono.just(new Review("abc", 1L, "Batman Begins", 9.0)));

        //when
        webTestClient.post()
                .uri(REVIEW_INFO_URL)
                .bodyValue(review)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Review.class)
                .consumeWith(reviewEntityExchangeResult -> {
                    var savedReview = reviewEntityExchangeResult.getResponseBody();
                    assertNotNull(savedReview);
                    assertEquals(1L, savedReview.getMovieInfoId());
                    assertEquals("abc", savedReview.getReviewId());
                    assertEquals(9.0, savedReview.getRating());
                });
    }

    @Test
    void updateReview() {
        var reviewId = "abc";
        var existingReview = new Review(reviewId, 1L, "Batman Begins", 9.0);
        var incomingReview = new Review(null, 1L, "Batman Begins - updated", 8.5);

        when(reviewRepository.findById(reviewId)).thenReturn(Mono.just(existingReview));
        when(requestValidator.validate(isA(Review.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(reviewRepository.save(isA(Review.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        //when
        webTestClient.put()
                .uri(REVIEW_INFO_URL + "/{id}", reviewId)
                .bodyValue(incomingReview)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Review.class)
                .consumeWith(reviewEntityExchangeResult -> {
                    var updatedReview = reviewEntityExchangeResult.getResponseBody();
                    assertNotNull(updatedReview);
                    assertEquals(reviewId, updatedReview.getReviewId());
                    assertEquals("Batman Begins - updated", updatedReview.getComment());
                    assertEquals(8.5, updatedReview.getRating());
                });
    }

    @Test
    void updateReview_notFound() {
        var reviewId = "unknownId";
        var incomingReview = new Review(null, 1L, "Batman Begins - updated", 8.5);

        when(reviewRepository.findById(reviewId)).thenReturn(Mono.empty());

        //when
        webTestClient.put()
                .uri(REVIEW_INFO_URL + "/{id}", reviewId)
                .bodyValue(incomingReview)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Review not found for the given reviewId: " + reviewId);

        verify(reviewRepository, never()).save(isA(Review.class));
    }

    @Test
    void deleteReview() {
        var reviewId = "abc";
        var existingReview = new Review(reviewId, 1L, "Batman Begins", 9.0);

        when(reviewRepository.findById(reviewId)).thenReturn(Mono.just(existingReview));
        when(reviewRepository.delete(isA(Review.class))).thenReturn(Mono.empty());

        //when
        webTestClient.delete()
                .uri(REVIEW_INFO_URL + "/{id}", reviewId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(reviewId)
                .jsonPath("$.message").isEqualTo("Review with id " + reviewId + " was successfully deleted");

        verify(reviewRepository).delete(existingReview);
    }

    @Test
    void deleteReview_notFound() {
        var reviewId = "unknownId";

        when(reviewRepository.findById(reviewId)).thenReturn(Mono.empty());

        //when
        webTestClient.delete()
                .uri(REVIEW_INFO_URL + "/{id}", reviewId)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Review not found for the given reviewId: " + reviewId);

        verify(reviewRepository, never()).delete(isA(Review.class));
    }
}
