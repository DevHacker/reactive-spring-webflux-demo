package com.gtechsolutions.movieinfoservice.exception;

public class MovieInfoNotFoundException extends RuntimeException {

    public MovieInfoNotFoundException(String message) {
        super(message);
    }
}
