package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FilmResponse {
    private Long id;
    private String titre;
    private String genre;
    private Integer duree;
    private String affiche;
    private String description;
}