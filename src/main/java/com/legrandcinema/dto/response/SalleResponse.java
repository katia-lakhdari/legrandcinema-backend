package com.legrandcinema.dto.response;

import com.legrandcinema.entity.Salle;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalleResponse {

    private Long id;
    private String nom;
    private String type;
    private Integer capacite;

    public SalleResponse(Salle salle) {
        this.id = salle.getId();
        this.nom = salle.getNom();
        this.type = salle.getType().name();
        this.capacite = salle.getCapacite();
    }
}