package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class IntentionPaiementResponse {

    private Long reservationId;
    private String paymentIntentId;
    private String clientSecret;
    private BigDecimal montant;
}