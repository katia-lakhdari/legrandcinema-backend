package com.legrandcinema.dto.response;

import com.legrandcinema.entity.Film;
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

    public FilmResponse(Film film) {
        this.id = film.getId();
        this.titre = film.getTitre();
        this.genre = film.getGenre();
        this.duree = film.getDuree();
        this.affiche = film.getAffiche();
        this.description = film.getDescription();
    }
}