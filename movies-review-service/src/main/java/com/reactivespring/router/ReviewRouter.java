package com.reactivespring.router;

import com.reactivespring.handler.ReviewHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
@RequiredArgsConstructor
public class ReviewRouter {

    @Bean
    public RouterFunction<ServerResponse> reviewRoutes(ReviewHandler reviewHandler) {
        return RouterFunctions.route()
                .path("/api/reviews",builder ->
                        builder.nest(accept(APPLICATION_JSON), nestedBuilder ->
                                nestedBuilder.GET("/{movieInfoId}", reviewHandler::getReviewsById)
                                        .PUT("/{id}", reviewHandler::updateReview)
                                        .DELETE("/{id}", reviewHandler::deleteReview))
                .POST( reviewHandler::createReview)
                .GET(reviewHandler::getAllReviews))
                .build();
    }
}
