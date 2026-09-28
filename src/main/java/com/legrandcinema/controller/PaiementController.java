package com.legrandcinema.controller;

import com.legrandcinema.dto.request.ConfirmationPaiementRequest;
import com.legrandcinema.dto.request.PaiementRequest;
import com.legrandcinema.dto.response.IntentionPaiementResponse;
import com.legrandcinema.dto.response.PaiementResponse;
import com.legrandcinema.service.PaiementService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @PostMapping("/intention")
    public IntentionPaiementResponse creerIntentionPaiement(@Valid @RequestBody PaiementRequest requete,
                                                            Authentication authentication) {
        return paiementService.creerIntentionPaiement(requete, authentication.getName());
    }

    @PostMapping("/confirmation")
    public PaiementResponse confirmerPaiement(@Valid @RequestBody ConfirmationPaiementRequest requete,
                                              Authentication authentication) {
        return paiementService.confirmerPaiement(requete, authentication.getName());
    }
}