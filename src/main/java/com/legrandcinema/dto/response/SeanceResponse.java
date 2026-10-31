package com.legrandcinema.dto.response;

import com.legrandcinema.entity.Seance;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class SeanceResponse {

    private final Long id;
    private final Long filmId;
    private final String titreFilm;
    private final Long salleId;
    private final String nomSalle;
    private final LocalDateTime dateHeure;
    private final BigDecimal prix;
    private final long placesLibres;
    private final boolean complete;

    public SeanceResponse(Seance seance, long placesLibres) {
        this.id = seance.getId();
        this.filmId = seance.getFilm().getId();
        this.titreFilm = seance.getFilm().getTitre();
        this.salleId = seance.getSalle().getId();
        this.nomSalle = seance.getSalle().getNom();
        this.dateHeure = seance.getDateHeure();
        this.prix = seance.getPrix();
        this.placesLibres = placesLibres;
        this.complete = placesLibres == 0;
    }
}