package com.legrandcinema.dto.response;

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
}