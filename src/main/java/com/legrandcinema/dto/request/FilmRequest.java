package com.legrandcinema.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilmRequest {
    private String titre;
    private Integer duree;
    private String affiche;
    private String description;
}