package com.legrandcinema.controller;

import com.legrandcinema.dto.request.SeanceRequest;
import com.legrandcinema.dto.response.SeanceResponse;
import com.legrandcinema.entity.Seance;
import com.legrandcinema.service.SeanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seances")
public class SeanceController {

    private final SeanceService seanceService;

    public SeanceController(SeanceService seanceService) {
        this.seanceService = seanceService;
    }

    @GetMapping
    public List<SeanceResponse> listerSeances() {
        return seanceService.listerToutesLesSeances().stream()
                .map(this::construireReponse)
                .toList();
    }

    @GetMapping("/{id}")
    public SeanceResponse trouverSeance(@PathVariable Long id) {
        Seance seance = seanceService.trouverParId(id);
        return construireReponse(seance);
    }

    @PostMapping
    public SeanceResponse creerSeance(@Valid @RequestBody SeanceRequest request) {
        Seance seance = seanceService.creerSeance(request);
        return construireReponse(seance);
    }

    @PutMapping("/{id}")
    public SeanceResponse modifierSeance(@PathVariable Long id, @Valid @RequestBody SeanceRequest request) {
        Seance seance = seanceService.modifierSeance(id, request);
        return construireReponse(seance);
    }

    @DeleteMapping("/{id}")
    public void supprimerSeance(@PathVariable Long id) {
        seanceService.supprimerSeance(id);
    }

    private SeanceResponse construireReponse(Seance seance) {
        long placesLibres = seanceService.compterPlacesLibres(seance.getId());
        return new SeanceResponse(seance, placesLibres);
    }
}