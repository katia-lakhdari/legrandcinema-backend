package com.legrandcinema.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaiementRequest {

    @NotNull(message = "L'identifiant de la réservation est obligatoire")
    private Long reservationId;
}