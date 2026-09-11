package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReservationAdminResponse {

    private Long reservationId;
    private String clientNom;
    private String filmTitre;
    private LocalDateTime dateHeureSeance;
    private int nombrePlaces;
    private String statut;
}