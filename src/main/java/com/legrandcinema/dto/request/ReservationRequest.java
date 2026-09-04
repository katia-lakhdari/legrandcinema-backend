package com.legrandcinema.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReservationRequest {

    @NotNull(message = "L'identifiant de la séance est obligatoire")
    private Long seanceId;

    @NotEmpty(message = "Au moins une place doit être sélectionnée")
    private List<Long> placeIds;
}