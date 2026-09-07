package com.legrandcinema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PaiementResponse {

    private Long reservationId;
    private String statutPaiement;
    private BigDecimal montant;
    private String qrCode;
}