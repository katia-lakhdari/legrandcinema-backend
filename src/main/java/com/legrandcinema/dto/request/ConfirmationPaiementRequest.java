package com.legrandcinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmationPaiementRequest {

    @NotNull(message = "L'identifiant de la réservation est obligatoire")
    private Long reservationId;

    @NotBlank(message = "L'identifiant du paiement Stripe est obligatoire")
    private String paymentIntentId;
}