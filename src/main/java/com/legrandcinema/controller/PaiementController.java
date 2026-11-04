package com.legrandcinema.controller;

import com.legrandcinema.dto.request.ConfirmationPaiementRequest;
import com.legrandcinema.dto.request.PaiementRequest;
import com.legrandcinema.dto.response.IntentionPaiementResponse;
import com.legrandcinema.dto.response.PaiementResponse;
import com.legrandcinema.service.PaiementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paiements")
@Tag(name = "Paiements", description = "Paiement en ligne avec Stripe")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @Operation(summary = "Préparer le paiement Stripe et afficher le récapitulatif")
    @PostMapping("/intention")
    public IntentionPaiementResponse creerIntentionPaiement(@Valid @RequestBody PaiementRequest requete,
                                                            Authentication authentication) {
        return paiementService.creerIntentionPaiement(requete, authentication.getName());
    }

    @Operation(summary = "Confirmer le paiement et créer les billets")
    @PostMapping("/confirmation")
    public PaiementResponse confirmerPaiement(@Valid @RequestBody ConfirmationPaiementRequest requete,
                                              Authentication authentication) {
        return paiementService.confirmerPaiement(requete, authentication.getName());
    }
}