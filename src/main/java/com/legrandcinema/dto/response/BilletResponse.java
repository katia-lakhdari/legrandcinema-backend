package com.legrandcinema.dto.response;

import java.util.List;

public record BilletResponse(
        Long id,
        String qrCode,
        boolean scanne,
        String titreFilm,
        String dateHeureSeance,
        List<String> numerosPlaces
) {
}