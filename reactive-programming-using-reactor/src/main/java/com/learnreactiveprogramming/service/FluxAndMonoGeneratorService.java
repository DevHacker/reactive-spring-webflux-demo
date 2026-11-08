package com.learnreactiveprogramming.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.tools.shaded.net.bytebuddy.matcher.InheritedAnnotationMatcher;

import java.time.Duration;
import java.util.List;
import java.util.Random;


public class FluxAndMonoGeneratorService {
    public Flux<String> namesFlux() {
        return Flux.fromIterable(List.of("alex", "ben", "chloe")).log();
    }

    public Flux<String> namesFluxMap() {
        return Flux.fromIterable(List.of("alex", "ben", "chloe"))
                .map(String::toUpperCase)
                .log();
    }

    public Mono<String> nameMono() {
        return Mono.just("FIDELE DONVIDE").log();
    }

    public Flux<String> namesFlux_flatMap(int stringLenght) {
        return Flux.fromIterable(List.of("alex", "ben", "chloe"))
                .map(String::toUpperCase)
                .filter(s -> s.length() > stringLenght)
                .flatMap(this::splitString)
                .log();
    }

    public Flux<String> splitString(String name){
        var charArray = name.split("");
        return Flux.fromArray(charArray);
    }

    public Flux<String> splitStringWithDelay(int stringLenght){
        return Flux.fromIterable(List.of("alex", "ben", "chloe"))
                .map(String::toUpperCase)
                .filter(s -> s.length() > stringLenght)
                .concatMap(this::splitString_withDelay)
                .log();
    }

    public Flux<String> splitString_withDelay(String name){
        var charArray = name.split("");
        var delay = new Random().nextInt(2000);
        return Flux.fromArray(charArray)
                .delayElements(Duration.ofMillis(delay));
    }

    public Flux<String> explore_merge(){
        var flux1 = Flux.just("A", "B", "C").delayElements(Duration.ofMillis(100));
        var flux2 = Flux.just("D", "E", "F").delayElements(Duration.ofMillis(125));
        return Flux.merge(flux1, flux2).log();
    }

    public Flux<String> explore_zip(){
        var flux1 = Flux.just("A", "B", "C").delayElements(Duration.ofMillis(100));
        var flux2 = Flux.just("D", "E", "F").delayElements(Duration.ofMillis(125));
        return Flux.zip(flux1, flux2, (s1, s2) -> s1 + s2).log();
    }


    public static void main(String[] args) {
        FluxAndMonoGeneratorService fluxAndMonoGeneratorService = new FluxAndMonoGeneratorService();
        fluxAndMonoGeneratorService.namesFlux().subscribe(name -> System.out.println("Name is: " + name));

        fluxAndMonoGeneratorService.nameMono().subscribe(name -> System.out.println("Name is: " + name));

        fluxAndMonoGeneratorService.namesFluxMap().subscribe(name -> System.out.println("Name flux map element is: " + name));
    }
}
