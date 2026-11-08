package com.reactivespring.handler;

import com.reactivespring.domain.Review;
import com.reactivespring.exception.DeleteResponse;
import com.reactivespring.exception.ReviewNotFoundException;
import com.reactivespring.repository.ReviewRepository;
import com.reactivespring.utils.RequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewHandler {

    private final ReviewRepository reviewRepository;
    private final RequestValidator requestValidator;

    public Mono<ServerResponse> createReview(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Review.class)
                .switchIfEmpty(Mono.error(() -> new ServerWebInputException("RequestBody is required")))
                .flatMap(requestValidator::validate)
                .flatMap(reviewRepository::save).log()
                .flatMap(review -> ServerResponse.status(HttpStatus.CREATED).bodyValue(review))
                .log();

    }

    public Mono<ServerResponse> updateReview(ServerRequest serverRequest) {
        var reviewId = serverRequest.pathVariable("id");
        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(()-> new ReviewNotFoundException("Review not found for the given reviewId: " + reviewId)))
                .flatMap(extistingReview -> serverRequest.bodyToMono(Review.class)
                        .switchIfEmpty(Mono.error(() -> new ServerWebInputException("RequestBody is required")))
                        .flatMap(requestValidator::validate)
                        .map(incominReview ->{
                            extistingReview.setComment(incominReview.getComment());
                            extistingReview.setRating(incominReview.getRating());
                            return extistingReview;
                        })
                        .flatMap(reviewRepository::save).log()
                        .flatMap(updatedReview -> ServerResponse.ok().bodyValue(updatedReview))
                        .log()

                );
    }

    public Mono<ServerResponse> getAllReviews(ServerRequest serverRequest) {
        Flux<Review> reviews = reviewRepository.findAll().log();
        return ServerResponse.status(HttpStatus.OK).body(reviews, Review.class);
    }

    public Mono<ServerResponse> getReviewsById(ServerRequest serverRequest) {
        Flux<Review> reviews = Mono.fromCallable(() -> Long.valueOf(serverRequest.pathVariable("movieInfoId")))
                .onErrorResume(e -> Mono.error(new ServerWebInputException("Invalid movieId: " + serverRequest.pathVariable("movieId"))))
                .flatMapMany(reviewRepository::findAllByMovieInfoId)
                .switchIfEmpty(Mono.error(() -> new ReviewNotFoundException("Review not found for the given movieId: " + serverRequest.pathVariable("movieId"))))
                .log();

        return ServerResponse.status(HttpStatus.OK).body(reviews, Review.class);

    }

    public Mono<ServerResponse> deleteReview(ServerRequest serverRequest) {
        var reviewId = serverRequest.pathVariable("id");
        return reviewRepository.findById(reviewId).log()
                .switchIfEmpty(Mono.error(() -> new ReviewNotFoundException("Review not found for the given reviewId: " + reviewId)))
                .flatMap(reviewRepository::delete)
                .thenReturn(new DeleteResponse(reviewId, "Review with id %s was successfully deleted".formatted(reviewId)))
                .flatMap(deleteResponse -> ServerResponse.ok().bodyValue(deleteResponse))
                .log();

    }
}
