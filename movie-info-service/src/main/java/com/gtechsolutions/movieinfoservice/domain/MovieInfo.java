package com.gtechsolutions.movieinfoservice.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Document
public class MovieInfo {
    @MongoId
    private String movieInfoId;

    /*@NotBlank(message = "Movie's id is required")
    @Positive(message = "Movie's id must be a positive number")
    private Long movieId;*/

    @NotBlank(message = "Movie name is required")
    private String name;

    @Positive(message = "Movie year must be a positive number")
    @NotNull(message = "Movie year is required")
    private Integer year;
    private List< @NotBlank(message = "Movie cast is required")String> cast;
    private LocalDate releaseDate;
}
