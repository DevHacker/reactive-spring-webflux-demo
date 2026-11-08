package com.learnreactiveprogramming.service;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class FluxAndMonoGeneratorServiceTest {

    FluxAndMonoGeneratorService fluxAndMonoGeneratorService = new FluxAndMonoGeneratorService();

    @Test
    void namesFlux() {
        //given

        //when
        var fluxNames = fluxAndMonoGeneratorService.namesFlux();

        //then
        StepVerifier.create(fluxNames)
                .expectNext("alex","ben", "cloe")
                .verifyComplete();
    }

    @Test
    void namesFlux_map() {
        //given

        //when
        var fluxNames = fluxAndMonoGeneratorService.namesFluxMap();

        //then
        StepVerifier.create(fluxNames)
                .expectNext("ALEX","BEN")
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void namesFlux_flatMap() {
        //given
        int stringLenght = 3;

        //when
        var fluxNames = fluxAndMonoGeneratorService.namesFlux_flatMap(stringLenght);

        //then
        StepVerifier.create(fluxNames)
                .expectNext("A","L","E","X","C","H","L","O","E")
                .verifyComplete();
    }

    @Test
    void splitStringWithDelay() {
        int stringLenght = 3;

        //when
        var fluxNames = fluxAndMonoGeneratorService.splitStringWithDelay(stringLenght);

        //then
        StepVerifier.create(fluxNames)
                .expectNextCount(9)
                .verifyComplete();
    }

    @Test
    void explore_merge() {
        //given

        //when
        var fluxNames = fluxAndMonoGeneratorService.explore_merge();

        //then
        StepVerifier.create(fluxNames)
                .expectNext("A", "D", "B", "E", "C", "F")
                .verifyComplete();
    }

    @Test
    void explore_zip() {
        //given

        //when
        var fluxNames = fluxAndMonoGeneratorService.explore_zip();

        //then
        StepVerifier.create(fluxNames)
                .expectNext("AD", "BE", "CF")
                .verifyComplete();
    }
}
