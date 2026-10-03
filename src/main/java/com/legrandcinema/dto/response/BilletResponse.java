package com.legrandcinema.dto.response;

import java.util.List;

public record BilletResponse(
        Long id,
        String qrCode,
        boolean scanne,
        String nomClient,
        String titreFilm,
        String dateHeureSeance,
        String nomSalle,
        List<String> numerosPlaces
) {
}