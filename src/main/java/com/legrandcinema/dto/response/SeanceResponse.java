package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class SeanceResponse {

    private Long id;
    private String titreFilm;
    private String nomSalle;
    private LocalDateTime dateHeure;
    private BigDecimal prix;

}