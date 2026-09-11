package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TauxRemplissageResponse {

    private Long seanceId;
    private String filmTitre;
    private LocalDateTime dateHeure;
    private int placesReservees;
    private int placesDisponibles;
    private double tauxRemplissage;
}