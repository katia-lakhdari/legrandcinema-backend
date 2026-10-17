package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class IntentionPaiementResponse {

    private Long reservationId;
    private String paymentIntentId;
    private String clientSecret;
    private BigDecimal montant;
    private String titreFilm;
    private String affiche;
    private LocalDateTime dateHeureSeance;
    private List<String> numerosPlaces;
    private long secondesRestantes;
}