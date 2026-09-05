package com.legrandcinema.controller;

import com.legrandcinema.dto.request.PaiementRequest;
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

    @PostMapping
    public PaiementResponse traiterPaiement(@Valid @RequestBody PaiementRequest requete,
                                            Authentication authentication) {
        return paiementService.traiterPaiement(requete, authentication.getName());
    }
}