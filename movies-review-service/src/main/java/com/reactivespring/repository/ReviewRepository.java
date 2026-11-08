package com.reactivespring.repository;

import com.reactivespring.domain.Review;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ReviewRepository extends ReactiveCrudRepository<Review, String> {
    Flux<Review> findAllByMovieInfoId(Long movieInfoId);
}
