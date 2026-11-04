package com.legrandcinema.dto.response;

import com.legrandcinema.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PlaceResponse {
    private Long id;
    private String numero;
    private String statut;
    private LocalDateTime finVerrouillage;
    private String raisonBlocage;

    public PlaceResponse(Place place) {
        this.id = place.getId();
        this.numero = place.getNumero();
        this.statut = place.getStatut().name();
        this.finVerrouillage = place.getFinVerrouillage();
        this.raisonBlocage = place.getRaisonBlocage();
    }
}