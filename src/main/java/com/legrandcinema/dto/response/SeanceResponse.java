package com.legrandcinema.dto.response;

import com.legrandcinema.entity.Seance;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class SeanceResponse {

    private Long id;
    private Long filmId;
    private String titreFilm;
    private Long salleId;
    private String nomSalle;
    private LocalDateTime dateHeure;
    private BigDecimal prix;
    private long placesLibres;
    private boolean complete;

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